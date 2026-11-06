package com.roberto.climbingwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.dtos.LoginRequest;
import com.roberto.climbingwiki.dtos.LoginResponse;
import com.roberto.climbingwiki.dtos.UserRequest;
import com.roberto.climbingwiki.dtos.UserResponse;
import com.roberto.climbingwiki.exceptions.InvalidCredentialsException;
import com.roberto.climbingwiki.exceptions.InvalidOrExpiredTokenException;
import com.roberto.climbingwiki.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.UserRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import com.roberto.climbingwiki.security.JwtService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, userService, passwordEncoder, jwtService);
    }

    private User user(Long id, String username, String passwordHash, User.Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        return user;
    }

    @Test
    void register_forcesUserRoleRegardlessOfRequest() {
        UserRequest request =
                new UserRequest("alice", "alice@test.com", "password123", null, null, null, User.Role.ADMIN);
        when(userService.create(any())).thenReturn(
                new UserResponse(1L, "alice", "alice@test.com", null, null, null, User.Role.USER, null));
        User saved = user(1L, "alice", "hashed", User.Role.USER);
        saved.setEmail("alice@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(saved));

        authService.register(request);

        verify(userService).create(argThat(forwarded -> forwarded.role() == User.Role.USER));
    }

    @Test
    void register_generatesVerificationTokenWithExpiry() {
        when(userService.create(any())).thenReturn(
                new UserResponse(1L, "alice", "alice@test.com", null, null, null, User.Role.USER, null));
        User saved = user(1L, "alice", "hashed", User.Role.USER);
        saved.setEmail("alice@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(saved));

        authService.register(new UserRequest("alice", "alice@test.com", "password123", null, null, null, null));

        verify(userRepository).save(argThat(u -> u.getVerificationToken() != null
                && u.getVerificationTokenExpiresAt() != null
                && u.getVerificationTokenExpiresAt().isAfter(LocalDateTime.now())));
    }

    @Test
    void login_validCredentials_returnsTokenAndIssuesIt() {
        User existing = user(1L, "alice", "hashed", User.Role.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generate(any(AuthenticatedUser.class))).thenReturn("a.b.c");

        LoginResponse result = authService.login(new LoginRequest("alice", "password123"));

        assertThat(result.token()).isEqualTo("a.b.c");
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo(User.Role.USER);
    }

    @Test
    void login_unknownUsername_throwsInvalidCredentials() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User existing = user(1L, "alice", "hashed", User.Role.USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void verifyEmail_validToken_marksVerifiedAndClearsToken() {
        User existing = user(1L, "alice", "hashed", User.Role.USER);
        existing.setVerificationToken("tok-123");
        existing.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(1));
        when(userRepository.findByVerificationToken("tok-123")).thenReturn(Optional.of(existing));

        authService.verifyEmail("tok-123");

        verify(userRepository).save(argThat(u -> u.isEmailVerified()
                && u.getVerificationToken() == null
                && u.getVerificationTokenExpiresAt() == null));
    }

    @Test
    void verifyEmail_unknownToken_throwsInvalidOrExpiredToken() {
        when(userRepository.findByVerificationToken("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("ghost"))
                .isInstanceOf(InvalidOrExpiredTokenException.class);
    }

    @Test
    void verifyEmail_expiredToken_throwsInvalidOrExpiredToken() {
        User existing = user(1L, "alice", "hashed", User.Role.USER);
        existing.setVerificationToken("tok-123");
        existing.setVerificationTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByVerificationToken("tok-123")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.verifyEmail("tok-123"))
                .isInstanceOf(InvalidOrExpiredTokenException.class);
    }

    @Test
    void requestPasswordReset_existingEmail_returnsTokenAndPersistsExpiry() {
        User existing = user(1L, "alice", "hashed", User.Role.USER);
        existing.setEmail("alice@test.com");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(existing));

        String token = authService.requestPasswordReset("alice@test.com");

        assertThat(token).isNotBlank();
        verify(userRepository).save(argThat(u -> token.equals(u.getResetToken())
                && u.getResetTokenExpiresAt() != null
                && u.getResetTokenExpiresAt().isAfter(LocalDateTime.now())));
    }

    @Test
    void requestPasswordReset_unknownEmail_throwsUserNotFound() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.requestPasswordReset("ghost@test.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void resetPassword_validToken_updatesPasswordHashAndClearsToken() {
        User existing = user(1L, "alice", "old-hash", User.Role.USER);
        existing.setResetToken("tok-123");
        existing.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        when(userRepository.findByResetToken("tok-123")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");

        authService.resetPassword("tok-123", "newPassword123");

        verify(userRepository).save(argThat(u -> "new-hash".equals(u.getPasswordHash())
                && u.getResetToken() == null
                && u.getResetTokenExpiresAt() == null));
    }

    @Test
    void resetPassword_expiredToken_throwsInvalidOrExpiredToken() {
        User existing = user(1L, "alice", "old-hash", User.Role.USER);
        existing.setResetToken("tok-123");
        existing.setResetTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByResetToken("tok-123")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.resetPassword("tok-123", "newPassword123"))
                .isInstanceOf(InvalidOrExpiredTokenException.class);
    }
}
