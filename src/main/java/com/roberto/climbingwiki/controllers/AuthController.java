package com.roberto.climbingwiki.controllers;

import com.roberto.climbingwiki.dtos.ForgotPasswordRequest;
import com.roberto.climbingwiki.dtos.ForgotPasswordResponse;
import com.roberto.climbingwiki.dtos.LoginRequest;
import com.roberto.climbingwiki.dtos.LoginResponse;
import com.roberto.climbingwiki.dtos.ResetPasswordRequest;
import com.roberto.climbingwiki.dtos.UserRequest;
import com.roberto.climbingwiki.dtos.UserResponse;
import com.roberto.climbingwiki.dtos.VerifyEmailRequest;
import com.roberto.climbingwiki.exceptions.TooManyLoginAttemptsException;
import com.roberto.climbingwiki.security.ClientIpResolver;
import com.roberto.climbingwiki.security.LoginRateLimiterService;
import com.roberto.climbingwiki.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiterService loginRateLimiterService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(
            AuthService authService,
            LoginRateLimiterService loginRateLimiterService,
            ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.loginRateLimiterService = loginRateLimiterService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody UserRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (!loginRateLimiterService.tryConsume(clientIpResolver.resolve(httpRequest))) {
            throw new TooManyLoginAttemptsException();
        }
        return authService.login(request);
    }

    @PostMapping("/verify-email")
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return new ForgotPasswordResponse(authService.requestPasswordReset(request.email()));
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
    }
}
