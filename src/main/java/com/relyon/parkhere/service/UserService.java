package com.relyon.parkhere.service;

import com.relyon.parkhere.dto.request.LoginRequest;
import com.relyon.parkhere.dto.request.RegisterRequest;
import com.relyon.parkhere.dto.request.UpdateUserRequest;
import com.relyon.parkhere.dto.response.AuthResponse;
import com.relyon.parkhere.dto.response.UserResponse;
import com.relyon.parkhere.exception.EmailAlreadyExistsException;
import com.relyon.parkhere.exception.InvalidCredentialsException;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.repository.UserRepository;
import com.relyon.parkhere.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        var user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        var savedUser = userRepository.save(user);
        var token = jwtService.generateToken(savedUser);
        log.info("New user registered: {}", savedUser.getEmail());
        return new AuthResponse(token, UserResponse.from(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(InvalidCredentialsException::new);

        var token = jwtService.generateToken(user);
        log.info("User logged in: {}", user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }

    public UserResponse getProfile(User user) {
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(User user, UpdateUserRequest request) {
        user.setName(request.name());
        var updatedUser = userRepository.save(user);
        log.info("User profile updated: {}", updatedUser.getEmail());
        return UserResponse.from(updatedUser);
    }
}
