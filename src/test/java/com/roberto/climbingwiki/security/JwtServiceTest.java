package com.roberto.climbingwiki.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.roberto.climbingwiki.model.entities.User;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-at-least-32-characters-long-for-hs256";

    private JwtService jwtService(long expirationMs) {
        return new JwtService(SECRET, expirationMs);
    }

    @Test
    void generate_thenParse_roundTripsClaims() {
        JwtService jwtService = jwtService(60_000);
        AuthenticatedUser user = new AuthenticatedUser(1L, "alice", User.Role.USER);

        String token = jwtService.generate(user);
        AuthenticatedUser parsed = jwtService.parse(token);

        assertThat(parsed).isEqualTo(user);
    }

    @Test
    void parse_tamperedToken_throws() {
        JwtService jwtService = jwtService(60_000);
        String token = jwtService.generate(new AuthenticatedUser(1L, "alice", User.Role.USER));
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void parse_expiredToken_throws() throws InterruptedException {
        JwtService jwtService = jwtService(1);
        String token = jwtService.generate(new AuthenticatedUser(1L, "alice", User.Role.USER));
        Thread.sleep(20);

        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void parse_garbageString_throws() {
        JwtService jwtService = jwtService(60_000);

        assertThatThrownBy(() -> jwtService.parse("not-a-jwt")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void generate_preservesRole() {
        JwtService jwtService = jwtService(60_000);
        AuthenticatedUser admin = new AuthenticatedUser(2L, "boss", User.Role.ADMIN);

        String token = jwtService.generate(admin);
        AuthenticatedUser parsed = jwtService.parse(token);

        assertThat(parsed.role()).isEqualTo(User.Role.ADMIN);
    }

    @Test
    void constructor_missingSecret_failsStartupWithClearMessage() {
        assertThatThrownBy(() -> new JwtService("", 60_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new JwtService(null, 60_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void constructor_secretShorterThan32Bytes_failsStartup() {
        assertThatThrownBy(() -> new JwtService("too-short-secret", 60_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
