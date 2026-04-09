package com.relyon.parkhere.service;

import com.relyon.parkhere.model.EmailVerificationToken;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.repository.EmailVerificationTokenRepository;
import com.relyon.parkhere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${parkhere.reset.base-url:http://localhost:3000/reset-password}")
    private String baseUrl;

    public void sendVerificationEmail(User user) {
        var token = EmailVerificationToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        tokenRepository.save(token);

        var verifyUrl = baseUrl.replace("/reset-password", "/verify-email") + "?token=" + token.getToken();

        var message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("ParkHere - Verificação de Email");
        message.setText("Olá " + user.getName() + ",\n\nClique no link abaixo para verificar seu email:\n\n" + verifyUrl + "\n\nEste link expira em 24 horas.\n\n— ParkHere");

        try {
            mailSender.send(message);
            log.info("Verification email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Transactional
    public boolean verify(String tokenValue) {
        var tokenOpt = tokenRepository.findByToken(tokenValue);
        if (tokenOpt.isEmpty()) {
            log.debug("Verification token not found: {}", tokenValue);
            return false;
        }

        var token = tokenOpt.get();
        if (token.isUsed() || token.isExpired()) {
            log.debug("Verification token expired or used: {}", tokenValue);
            return false;
        }

        token.setUsed(true);
        tokenRepository.save(token);

        var user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified for user {}", user.getEmail());
        return true;
    }
}
