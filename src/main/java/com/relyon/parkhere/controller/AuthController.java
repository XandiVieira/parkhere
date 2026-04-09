package com.relyon.parkhere.controller;

import com.relyon.parkhere.dto.request.ForgotPasswordRequest;
import com.relyon.parkhere.dto.request.GoogleAuthRequest;
import com.relyon.parkhere.dto.request.LoginRequest;
import com.relyon.parkhere.dto.request.RegisterRequest;
import com.relyon.parkhere.dto.request.ResetPasswordRequest;
import com.relyon.parkhere.dto.response.AuthResponse;
import com.relyon.parkhere.service.EmailVerificationService;
import com.relyon.parkhere.service.GoogleAuthService;
import com.relyon.parkhere.service.LocalizedMessageService;
import com.relyon.parkhere.service.PasswordResetService;
import com.relyon.parkhere.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Auth", description = "Authentication and password management")
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final GoogleAuthService googleAuthService;
    private final EmailVerificationService emailVerificationService;
    private final LocalizedMessageService messageService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(googleAuthService.authenticateWithGoogle(request.credential()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(Map.of("message", messageService.translate("reset.email.sent")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", messageService.translate("reset.password.success")));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        var verified = emailVerificationService.verify(token);
        if (verified) {
            return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired verification link"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody Map<String, String> body) {
        var email = body.get("email");
        if (email != null) {
            userService.findByEmail(email).ifPresent(emailVerificationService::sendVerificationEmail);
        }
        return ResponseEntity.ok(Map.of("message", "If this email is registered, a verification email has been sent"));
    }
}
