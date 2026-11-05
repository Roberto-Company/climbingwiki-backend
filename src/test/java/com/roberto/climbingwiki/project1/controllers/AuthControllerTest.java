package com.roberto.climbingwiki.project1.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roberto.climbingwiki.project1.dtos.LoginResponse;
import com.roberto.climbingwiki.project1.dtos.UserResponse;
import com.roberto.climbingwiki.project1.exceptions.InvalidCredentialsException;
import com.roberto.climbingwiki.project1.exceptions.InvalidOrExpiredTokenException;
import com.roberto.climbingwiki.project1.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.User;
import com.roberto.climbingwiki.project1.security.ClientIpResolver;
import com.roberto.climbingwiki.project1.security.LoginRateLimiterService;
import com.roberto.climbingwiki.project1.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final String REGISTER_BODY =
            "{\"username\":\"alice\",\"email\":\"alice@test.com\",\"password\":\"password123\"}";
    private static final String LOGIN_BODY = "{\"username\":\"alice\",\"password\":\"password123\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private LoginRateLimiterService loginRateLimiterService;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @BeforeEach
    void allowLoginAttemptsByDefault() {
        when(clientIpResolver.resolve(any())).thenReturn("198.51.100.7");
        when(loginRateLimiterService.tryConsume(any())).thenReturn(true);
    }

    @Test
    void register_validRequest_returns201() throws Exception {
        when(authService.register(any()))
                .thenReturn(new UserResponse(1L, "alice", "alice@test.com", null, null, null, User.Role.USER, null));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"email\":\"alice@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("a.b.c", 1L, "alice", User.Role.USER));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("a.b.c"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_rateLimitsByResolvedClientIp() throws Exception {
        when(authService.login(any())).thenReturn(null);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY));

        verify(loginRateLimiterService).tryConsume("198.51.100.7");
    }

    @Test
    void login_rateLimitExceeded_returns429() throws Exception {
        when(loginRateLimiterService.tryConsume(any())).thenReturn(false);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void verifyEmail_validToken_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok-123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void verifyEmail_invalidToken_returns400() throws Exception {
        doThrow(new InvalidOrExpiredTokenException()).when(authService).verifyEmail("ghost");

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"ghost\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_blankToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPassword_existingEmail_returnsToken() throws Exception {
        when(authService.requestPasswordReset("alice@test.com")).thenReturn("reset-tok");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").value("reset-tok"));
    }

    @Test
    void forgotPassword_unknownEmail_returns404() throws Exception {
        when(authService.requestPasswordReset("ghost@test.com")).thenThrow(new UserNotFoundException("ghost@test.com"));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@test.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void forgotPassword_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_validToken_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok-123\",\"newPassword\":\"newPassword123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_invalidToken_returns400() throws Exception {
        doThrow(new InvalidOrExpiredTokenException())
                .when(authService)
                .resetPassword("ghost", "newPassword123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"ghost\",\"newPassword\":\"newPassword123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok-123\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }
}
