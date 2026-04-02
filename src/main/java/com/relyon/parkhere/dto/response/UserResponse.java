package com.relyon.parkhere.dto.response;

import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        double reputationScore,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getReputationScore(),
                user.getCreatedAt()
        );
    }
}
