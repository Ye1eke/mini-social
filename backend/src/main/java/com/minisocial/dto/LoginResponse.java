package com.minisocial.dto;

public record LoginResponse(
        String token,
        Long userId,
        String email
) {
}
