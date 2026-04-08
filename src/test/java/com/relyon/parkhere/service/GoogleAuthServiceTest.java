package com.relyon.parkhere.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.relyon.parkhere.dto.response.AuthResponse;
import com.relyon.parkhere.exception.InvalidCredentialsException;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.repository.UserRepository;
import com.relyon.parkhere.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private GoogleAuthService googleAuthService;

    private User buildUser(String email, String provider, String providerId) {
        var user = User.builder()
                .id(UUID.randomUUID())
                .name("Google User")
                .email(email)
                .provider(provider)
                .providerId(providerId)
                .role(Role.USER)
                .active(true)
                .build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void authenticateWithGoogle_shouldThrowWhenTokenIsInvalid() {
        // The GoogleIdTokenVerifier.verify will fail with any random string since
        // there is no real Google infrastructure. The service catches all exceptions
        // and wraps them in InvalidCredentialsException.
        assertThrows(InvalidCredentialsException.class,
                () -> googleAuthService.authenticateWithGoogle("invalid-token-string"));
    }

    @Test
    void authenticateWithGoogle_shouldThrowWhenTokenIsNull() {
        assertThrows(InvalidCredentialsException.class,
                () -> googleAuthService.authenticateWithGoogle(null));
    }

    @Test
    void authenticateWithGoogle_shouldThrowWhenTokenIsEmpty() {
        assertThrows(InvalidCredentialsException.class,
                () -> googleAuthService.authenticateWithGoogle(""));
    }
}
