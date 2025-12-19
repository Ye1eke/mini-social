package com.minisocial.controller;

import com.minisocial.dto.FeedResponse;
import com.minisocial.security.AuthUser;
import com.minisocial.service.FeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for feed-related operations.
 */
@RestController
@RequestMapping("/feed")
public class FeedController {

    private static final Logger logger = LoggerFactory.getLogger(FeedController.class);

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    /**
     * Retrieves the personalized feed for the authenticated user.
     * 
     * @param page The page number (default: 0)
     * @param size The page size (default: 20)
     * @return FeedResponse with HTTP 200 status
     */
    @GetMapping
    public ResponseEntity<FeedResponse> getFeed(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @AuthenticationPrincipal AuthUser user) {

        logger.info("Received feed request for page: {}, size: {}", page, size);

        // Retrieve the feed
        FeedResponse response = feedService.getFeed(user.id(), page, size);
        logger.info("Feed retrieved successfully for user: {}, items: {}", user.id(), response.items().size());

        return ResponseEntity.ok(response);
    }
}
