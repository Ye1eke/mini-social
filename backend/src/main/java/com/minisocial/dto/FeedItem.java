package com.minisocial.dto;

import java.time.Instant;

public record FeedItem(
    Long postId,
    Long authorId,
    String content,
    String imageUrl,
    Instant createdAt
) {}
