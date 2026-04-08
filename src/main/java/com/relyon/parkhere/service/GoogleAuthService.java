package com.relyon.parkhere.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.relyon.parkhere.dto.response.AuthResponse;
import com.relyon.parkhere.dto.response.UserResponse;
import com.relyon.parkhere.exception.InvalidCredentialsException;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.repository.UserRepository;
import com.relyon.parkhere.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${parkhere.google.client-id}")
    private String googleClientId;

    @Transactional
    public AuthResponse authenticateWithGoogle(String idTokenString) {
        try {
            var verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            var idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new InvalidCredentialsException();
            }

            var payload = idToken.getPayload();
            var googleId = payload.getSubject();
            var email = payload.getEmail();
            var name = (String) payload.get("name");
            var pictureUrl = (String) payload.get("picture");

            // Find existing user by Google provider ID or by email
            var user = userRepository.findByProviderAndProviderId("GOOGLE", googleId)
                    .or(() -> userRepository.findByEmail(email))
                    .orElse(null);

            if (user == null) {
                // Create new user
                user = User.builder()
                        .name(name != null ? name : email)
                        .email(email)
                        .provider("GOOGLE")
                        .providerId(googleId)
                        .build();
                user = userRepository.save(user);
                log.info("New Google user registered: {}", email);
            } else if (user.getProvider() == null) {
                // Link existing email account to Google
                user.setProvider("GOOGLE");
                user.setProviderId(googleId);
                user = userRepository.save(user);
                log.info("Linked Google to existing account: {}", email);
            }

            var token = jwtService.generateToken(user);
            return new AuthResponse(token, UserResponse.from(user));

        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google authentication failed", e);
            throw new InvalidCredentialsException();
        }
    }
}
