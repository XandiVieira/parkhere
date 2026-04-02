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
import com.relyon.parkhere.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserResponse sampleUserResponse() {
        return new UserResponse(UUID.randomUUID(), "John", "john@test.com", Role.USER, 0.0, LocalDateTime.now());
    }

    @Test
    void register_shouldReturn201WithToken() throws Exception {
        var request = new RegisterRequest("John", "john@test.com", "password123");
        var response = new AuthResponse("jwt-token", sampleUserResponse());
        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
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

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_shouldReturn400ForInvalidInput() throws Exception {
        var request = new RegisterRequest("", "not-an-email", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn200WithToken() throws Exception {
        var request = new LoginRequest("john@test.com", "password123");
        var response = new AuthResponse("jwt-token", sampleUserResponse());
        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_shouldReturn401ForInvalidCredentials() throws Exception {
        var request = new LoginRequest("john@test.com", "wrong");
        when(userService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
