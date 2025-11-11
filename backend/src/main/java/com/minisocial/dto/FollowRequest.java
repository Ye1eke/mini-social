package com.minisocial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FollowRequest(
        @NotNull(message = "Target user ID is required")
        @Positive(message = "Target user ID must be positive")
        Long targetUserId
) {
}
