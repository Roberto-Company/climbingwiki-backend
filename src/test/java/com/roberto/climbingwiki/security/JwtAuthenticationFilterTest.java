package com.roberto.climbingwiki.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/zones");
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();

    /** What the chain saw: the filter clears nothing after the request, so read it from the holder. */
    private Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static User user(Long id, String username, User.Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthorizationHeader_continuesAnonymously() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(authentication()).isNull();
    }

    @Test
    void validToken_existingUser_authenticatesWithRoleFromDatabase() throws Exception {
        request.addHeader("Authorization", "Bearer good");
        when(jwtService.parse("good")).thenReturn(new AuthenticatedUser(7L, "alice", User.Role.MODERATOR));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "alice", User.Role.USER)));

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(authentication().getPrincipal()).isEqualTo(new AuthenticatedUser(7L, "alice", User.Role.USER));
        assertThat(authentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void validToken_deletedUser_answers401AndStops() throws Exception {
        request.addHeader("Authorization", "Bearer good");
        when(jwtService.parse("good")).thenReturn(new AuthenticatedUser(7L, "alice", User.Role.USER));
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
        assertThat(authentication()).isNull();
    }

    @Test
    void invalidOrExpiredToken_answers401AndStops() throws Exception {
        request.addHeader("Authorization", "Bearer expired");
        when(jwtService.parse("expired")).thenThrow(new InvalidTokenException());

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }
}
