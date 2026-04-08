package com.relyon.parkhere.dto.response;

import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String nickname,
        String email,
        Role role,
        double reputationScore,
        String profilePicUrl,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        var picUrl = user.getProfilePic() != null
                ? "/api/v1/images/" + user.getProfilePic()
                : null;
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getNickname(),
                user.getEmail(),
                user.getRole(),
                user.getReputationScore(),
                picUrl,
                user.getCreatedAt()
        );
    }
}
