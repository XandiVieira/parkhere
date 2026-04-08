package com.relyon.parkhere.service;

import com.relyon.parkhere.exception.InvalidResetTokenException;
import com.relyon.parkhere.model.PasswordResetToken;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.repository.PasswordResetTokenRepository;
import com.relyon.parkhere.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User buildUser() {
        var user = User.builder()
                .id(UUID.randomUUID()).name("John").email("john@test.com")
                .password("encoded").role(Role.USER).build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void requestReset_shouldGenerateTokenAndSendEmail() {
        var user = buildUser();
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> {
            var token = inv.<PasswordResetToken>getArgument(0);
            token.setId(UUID.randomUUID());
            return token;
        });

        passwordResetService.requestReset("john@test.com");

        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void requestReset_shouldDoNothingForUnknownEmail() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("unknown@test.com");

        verify(tokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void resetPassword_shouldUpdatePasswordAndMarkTokenUsed() {
        var user = buildUser();
        var token = PasswordResetToken.builder()
                .id(UUID.randomUUID()).user(user).token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(1)).used(false).build();
        when(tokenRepository.findByTokenAndUsedFalse(token.getToken())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

        passwordResetService.resetPassword(token.getToken(), "newPassword123");

        assertEquals("newEncoded", user.getPassword());
        assertTrue(token.isUsed());
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void resetPassword_shouldThrowWhenTokenNotFound() {
        var tokenId = UUID.randomUUID();
        when(tokenRepository.findByTokenAndUsedFalse(tokenId)).thenReturn(Optional.empty());

        assertThrows(InvalidResetTokenException.class,
                () -> passwordResetService.resetPassword(tokenId, "newPassword123"));
    }

    @Test
    void resetPassword_shouldThrowWhenTokenExpired() {
        var user = buildUser();
        var token = PasswordResetToken.builder()
                .id(UUID.randomUUID()).user(user).token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().minusHours(1)).used(false).build();
        when(tokenRepository.findByTokenAndUsedFalse(token.getToken())).thenReturn(Optional.of(token));

        assertThrows(InvalidResetTokenException.class,
                () -> passwordResetService.resetPassword(token.getToken(), "newPassword123"));
    }
}
