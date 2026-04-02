package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.request.LoginRequest;
import com.relyon.parkhere.dto.request.RegisterRequest;
import com.relyon.parkhere.dto.request.UpdateUserRequest;
import com.relyon.parkhere.exception.EmailAlreadyExistsException;
import com.relyon.parkhere.exception.InvalidCredentialsException;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.repository.UserRepository;
import com.relyon.parkhere.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private User buildUser() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .name("John")
                .email("john@test.com")
                .password("encoded")
                .role(Role.USER)
                .reputationScore(0.0)
                .active(true)
                .build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void register_shouldCreateUserAndReturnToken() {
        var request = new RegisterRequest("John", "john@test.com", "password123");
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            var user = inv.<User>getArgument(0);
            user.setId(UUID.randomUUID());
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        var response = userService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("John", response.user().name());
        assertEquals("john@test.com", response.user().email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowWhenEmailExists() {
        var request = new RegisterRequest("John", "john@test.com", "password123");
        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() {
        var request = new LoginRequest("john@test.com", "password123");
        var user = buildUser();
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        var response = userService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("john@test.com", response.user().email());
    }

    @Test
    void login_shouldThrowForInvalidPassword() {
        var request = new LoginRequest("john@test.com", "wrong");
        var user = buildUser();
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void login_shouldThrowForNonExistentEmail() {
        var request = new LoginRequest("noone@test.com", "password");
        when(userRepository.findByEmail("noone@test.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void getProfile_shouldReturnUserResponse() {
        var user = buildUser();

        var response = userService.getProfile(user);

        assertEquals(user.getName(), response.name());
        assertEquals(user.getEmail(), response.email());
    }

    @Test
    void updateProfile_shouldUpdateName() {
        var user = buildUser();
        var request = new UpdateUserRequest("New Name");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = userService.updateProfile(user, request);

        assertEquals("New Name", response.name());
        verify(userRepository).save(user);
    }
}
