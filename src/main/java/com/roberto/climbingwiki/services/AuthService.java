package com.roberto.climbingwiki.services;

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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofHours(24);
    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(UserRequest request) {
        UserRequest forcedUserRole = new UserRequest(
                request.username(),
                request.email(),
                request.password(),
                request.fullName(),
                request.bio(),
                request.avatarUrl(),
                User.Role.USER);
        UserResponse created = userService.create(forcedUserRole);

        User user = userRepository.findById(created.id()).orElseThrow(() -> new UserNotFoundException(created.id()));
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plus(VERIFICATION_TOKEN_TTL));
        userRepository.save(user);
        log.info("Verification token for {}: {}", user.getEmail(), token);

        return created;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository
                .findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole());
        String token = jwtService.generate(authenticatedUser);
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token).orElseThrow(InvalidOrExpiredTokenException::new);
        if (user.getVerificationTokenExpiresAt() == null
                || user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOrExpiredTokenException();
        }
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public String requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiresAt(LocalDateTime.now().plus(RESET_TOKEN_TTL));
        userRepository.save(user);
        log.info("Password reset token for {}: {}", user.getEmail(), token);

        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token).orElseThrow(InvalidOrExpiredTokenException::new);
        if (user.getResetTokenExpiresAt() == null || user.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOrExpiredTokenException();
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);
    }
}
