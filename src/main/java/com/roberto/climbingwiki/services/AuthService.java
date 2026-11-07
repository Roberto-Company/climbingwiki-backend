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
import com.roberto.climbingwiki.services.mail.AccountMailer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofHours(24);
    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountMailer accountMailer;

    public AuthService(
            UserRepository userRepository,
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AccountMailer accountMailer) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accountMailer = accountMailer;
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
        accountMailer.sendEmailVerification(user.getEmail(), user.getUsername(), token);

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

    /**
     * Mails a reset link if the email belongs to an account. Unknown emails are ignored silently so
     * the endpoint can't be used to find out who is registered.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiresAt(LocalDateTime.now().plus(RESET_TOKEN_TTL));
            userRepository.save(user);
            accountMailer.sendPasswordReset(user.getEmail(), user.getUsername(), token);
        });
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
