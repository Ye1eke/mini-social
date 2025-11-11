package com.minisocial.dto;

import java.util.List;

public record FeedResponse(
    List<FeedItem> items,
    Integer page,
    Integer size
) {}
