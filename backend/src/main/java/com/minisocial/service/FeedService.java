package com.minisocial.service;

import com.minisocial.dto.FeedResponse;

public interface FeedService {
    FeedResponse getFeed(Long userId, Integer page, Integer size);
}
