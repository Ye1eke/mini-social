package com.minisocial.dto;

import java.time.Instant;

public record CreatePostResponse(
        Long postId,
        Long userId,
        String content,
        String imageUrl,
        Instant createdAt
) {
}
