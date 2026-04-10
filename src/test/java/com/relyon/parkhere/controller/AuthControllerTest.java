package com.relyon.parkhere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.parkhere.config.SecurityConfig;
import com.relyon.parkhere.dto.request.LoginRequest;
import com.relyon.parkhere.dto.request.RegisterRequest;
import com.relyon.parkhere.dto.response.AuthResponse;
import com.relyon.parkhere.dto.response.UserResponse;
import com.relyon.parkhere.exception.EmailAlreadyExistsException;
import com.relyon.parkhere.exception.InvalidCredentialsException;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.security.JwtService;
import com.relyon.parkhere.service.LocalizedMessageService;
import com.relyon.parkhere.service.PasswordResetService;
import com.relyon.parkhere.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.relyon.parkhere.model.User;
import com.relyon.parkhere.service.EmailVerificationService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private com.relyon.parkhere.service.GoogleAuthService googleAuthService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private LocalizedMessageService localizedMessageService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    private UserResponse sampleUserResponse() {
        return new UserResponse(UUID.randomUUID(), "John", null, "john@test.com", Role.USER, 0.0, null, false, LocalDateTime.now());
    }

    @Test
    void register_shouldReturn201WithToken() throws Exception {
        var request = new RegisterRequest("John", "john@test.com", "password123");
        var response = new AuthResponse("jwt-token", sampleUserResponse());
        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.name").value("John"));
    }

    @Test
    void register_shouldReturn409WhenEmailExists() throws Exception {
        var request = new RegisterRequest("John", "john@test.com", "password123");
        when(userService.register(any(RegisterRequest.class))).thenThrow(new EmailAlreadyExistsException("john@test.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_shouldReturn400ForInvalidInput() throws Exception {
        var request = new RegisterRequest("", "not-an-email", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn200WithToken() throws Exception {
        var request = new LoginRequest("john@test.com", "password123");
        var response = new AuthResponse("jwt-token", sampleUserResponse());
        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_shouldReturn401ForInvalidCredentials() throws Exception {
        var request = new LoginRequest("john@test.com", "wrong");
        when(userService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPassword_shouldReturn200Always() throws Exception {
        when(localizedMessageService.translate("reset.email.sent")).thenReturn("If this email is registered, a reset link has been sent");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"john@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void forgotPassword_shouldReturn400ForInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_shouldReturn200OnSuccess() throws Exception {
        when(localizedMessageService.translate("reset.password.success")).thenReturn("Password has been reset");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + UUID.randomUUID() + "\", \"newPassword\": \"newPassword123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void verifyEmail_shouldReturn200WhenTokenValid() throws Exception {
        when(emailVerificationService.verify("valid-token")).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
    }

    @Test
    void verifyEmail_shouldReturn400WhenTokenInvalid() throws Exception {
        when(emailVerificationService.verify("bad-token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", "bad-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired verification link"));
    }

    @Test
    void resendVerification_shouldReturn200Always() throws Exception {
        var user = User.builder()
                .id(UUID.randomUUID())
                .name("John")
                .email("john@test.com")
                .password("encoded")
                .build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        when(userService.findByEmail("john@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"john@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(emailVerificationService).sendVerificationEmail(user);
    }

    @Test
    void resendVerification_shouldReturn200EvenWhenEmailNotFound() throws Exception {
        when(userService.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"unknown@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(emailVerificationService, never()).sendVerificationEmail(any());
    }
}
