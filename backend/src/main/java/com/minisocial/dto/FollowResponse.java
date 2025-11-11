package com.minisocial.dto;

import java.time.Instant;

public record FollowResponse(
        Long followerId,
        Long followingId,
        Instant createdAt
) {
}
