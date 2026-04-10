package com.relyon.parkhere.service;

import com.relyon.parkhere.model.EmailVerificationToken;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.repository.EmailVerificationTokenRepository;
import com.relyon.parkhere.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User buildUser() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .name("John")
                .email("john@test.com")
                .password("encoded")
                .role(Role.USER)
                .emailVerified(false)
                .build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void sendVerificationEmail_shouldSaveTokenAndSendEmail() {
        ReflectionTestUtils.setField(emailVerificationService, "baseUrl", "http://localhost:3000/reset-password");
        var user = buildUser();
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        emailVerificationService.sendVerificationEmail(user);

        var tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        var savedToken = tokenCaptor.getValue();
        assertEquals(user, savedToken.getUser());
        assertNotNull(savedToken.getToken());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));
        assertFalse(savedToken.isUsed());

        var messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        var sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage.getTo());
        assertEquals("john@test.com", sentMessage.getTo()[0]);
        assertTrue(sentMessage.getText().contains("verify-email"));
    }

    @Test
    void sendVerificationEmail_shouldNotThrowWhenMailFails() {
        ReflectionTestUtils.setField(emailVerificationService, "baseUrl", "http://localhost:3000/reset-password");
        var user = buildUser();
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailVerificationService.sendVerificationEmail(user));

        verify(tokenRepository).save(any(EmailVerificationToken.class));
    }

    @Test
    void verify_shouldReturnTrueAndMarkUserVerified() {
        var user = buildUser();
        var token = EmailVerificationToken.builder()
                .user(user)
                .token("valid-token")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        token.setCreatedAt(LocalDateTime.now());
        token.setUpdatedAt(LocalDateTime.now());

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        var result = emailVerificationService.verify("valid-token");

        assertTrue(result);
        assertTrue(token.isUsed());
        assertTrue(user.isEmailVerified());
        verify(tokenRepository).save(token);
        verify(userRepository).save(user);
    }

    @Test
    void verify_shouldReturnFalseWhenTokenNotFound() {
        when(tokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        var result = emailVerificationService.verify("nonexistent");

        assertFalse(result);
        verify(tokenRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verify_shouldReturnFalseWhenTokenAlreadyUsed() {
        var user = buildUser();
        var token = EmailVerificationToken.builder()
                .user(user)
                .token("used-token")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(true)
                .build();
        token.setCreatedAt(LocalDateTime.now());
        token.setUpdatedAt(LocalDateTime.now());

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        var result = emailVerificationService.verify("used-token");

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void verify_shouldReturnFalseWhenTokenExpired() {
        var user = buildUser();
        var token = EmailVerificationToken.builder()
                .user(user)
                .token("expired-token")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .used(false)
                .build();
        token.setCreatedAt(LocalDateTime.now());
        token.setUpdatedAt(LocalDateTime.now());

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        var result = emailVerificationService.verify("expired-token");

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }
}
