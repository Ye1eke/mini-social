package com.minisocial.controller;

import com.minisocial.dto.FeedResponse;
import com.minisocial.exception.UnauthorizedException;
import com.minisocial.service.FeedService;
import com.minisocial.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final JwtUtil jwtUtil;

    public FeedController(FeedService feedService, JwtUtil jwtUtil) {
        this.feedService = feedService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Retrieves the personalized feed for the authenticated user.
     * 
     * @param page The page number (default: 0)
     * @param size The page size (default: 20)
     * @param httpRequest The HTTP request to extract JWT token
     * @return FeedResponse with HTTP 200 status
     */
    @GetMapping
    public ResponseEntity<FeedResponse> getFeed(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest httpRequest) {

        logger.info("Received feed request for page: {}, size: {}", page, size);

        // Extract JWT token from Authorization header
        String token = extractJwtToken(httpRequest);
        if (token == null) {
            throw new UnauthorizedException("Missing or invalid authorization token");
        }

        // Extract user ID from JWT token
        Long userId;
        try {
            userId = jwtUtil.extractUserId(token);
            if (userId == null) {
                throw new UnauthorizedException("Invalid token: user ID not found");
            }
        } catch (Exception e) {
            logger.error("Failed to extract user ID from token", e);
            throw new UnauthorizedException("Invalid or expired token");
        }

        // Retrieve the feed
        FeedResponse response = feedService.getFeed(userId, page, size);
        logger.info("Feed retrieved successfully for user: {}, items: {}", userId, response.items().size());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Extracts JWT token from the Authorization header.
     * 
     * @param request The HTTP request
     * @return JWT token string or null if not found
     */
    private String extractJwtToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
