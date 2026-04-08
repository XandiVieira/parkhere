package com.relyon.parkhere.service;

import com.relyon.parkhere.exception.InvalidResetTokenException;
import com.relyon.parkhere.model.PasswordResetToken;
import com.relyon.parkhere.repository.PasswordResetTokenRepository;
import com.relyon.parkhere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${parkhere.reset.base-url:http://localhost:3000/reset-password}")
    private String resetBaseUrl;

    static final int TOKEN_EXPIRY_HOURS = 1;

    @Transactional
    public void requestReset(String email) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.debug("Password reset requested for unknown email: {}", email);
            return;
        }

        var user = userOpt.get();
        var resetToken = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .build();

        tokenRepository.save(resetToken);

        var message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("ParkHere - Password Reset");
        message.setText("Click the link below to reset your password:\n\n"
                + resetBaseUrl + "?token=" + resetToken.getToken()
                + "\n\nThis link expires in " + TOKEN_EXPIRY_HOURS + " hour(s)."
                + "\n\nIf you did not request this, ignore this email.");

        mailSender.send(message);
        log.info("Password reset email sent to {}", email);
    }

    @Transactional
    public void resetPassword(UUID token, String newPassword) {
        var resetToken = tokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(InvalidResetTokenException::new);

        if (resetToken.isExpired()) {
            throw new InvalidResetTokenException();
        }

        var user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password reset completed for user {}", user.getEmail());
    }
}
