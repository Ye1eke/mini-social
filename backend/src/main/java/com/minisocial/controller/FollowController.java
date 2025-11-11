package com.minisocial.controller;

import com.minisocial.dto.FollowRequest;
import com.minisocial.dto.FollowResponse;
import com.minisocial.exception.UnauthorizedException;
import com.minisocial.service.FollowService;
import com.minisocial.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for follow-related operations.
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    private static final Logger logger = LoggerFactory.getLogger(FollowController.class);

    private final FollowService followService;
    private final JwtUtil jwtUtil;

    public FollowController(FollowService followService, JwtUtil jwtUtil) {
        this.followService = followService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Creates a follow relationship for the authenticated user.
     * 
     * @param request The follow request containing target user ID
     * @param httpRequest The HTTP request to extract JWT token
     * @return FollowResponse with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<FollowResponse> followUser(
            @Valid @RequestBody FollowRequest request,
            HttpServletRequest httpRequest) {

        logger.info("Received follow request for target user ID: {}", request.targetUserId());

        // Extract JWT token from Authorization header
        String token = extractJwtToken(httpRequest);
        if (token == null) {
            throw new UnauthorizedException("Missing or invalid authorization token");
        }

        // Extract user ID from JWT token
        Long followerId;
        try {
            followerId = jwtUtil.extractUserId(token);
            if (followerId == null) {
                throw new UnauthorizedException("Invalid token: user ID not found");
            }
        } catch (Exception e) {
            logger.error("Failed to extract user ID from token", e);
            throw new UnauthorizedException("Invalid or expired token");
        }

        // Create the follow relationship
        FollowResponse response = followService.followUser(followerId, request.targetUserId());
        logger.info("Follow relationship created successfully: {} -> {}", followerId, request.targetUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
