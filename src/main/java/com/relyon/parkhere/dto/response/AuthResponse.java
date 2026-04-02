package com.relyon.parkhere.dto.response;

public record AuthResponse(
        String token,
        UserResponse user
) {}
