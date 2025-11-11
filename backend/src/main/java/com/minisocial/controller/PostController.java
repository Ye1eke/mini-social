package com.minisocial.controller;

import com.minisocial.dto.CreatePostRequest;
import com.minisocial.dto.CreatePostResponse;
import com.minisocial.exception.UnauthorizedException;
import com.minisocial.service.PostService;
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
 * REST controller for post-related operations.
 */
@RestController
@RequestMapping("/posts")
public class PostController {

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    private final PostService postService;
    private final JwtUtil jwtUtil;

    public PostController(PostService postService, JwtUtil jwtUtil) {
        this.postService = postService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Creates a new post for the authenticated user.
     * 
     * @param request The post creation request
     * @param httpRequest The HTTP request to extract JWT token
     * @return CreatePostResponse with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<CreatePostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            HttpServletRequest httpRequest) {

        logger.info("Received post creation request");

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

        // Create the post
        CreatePostResponse response = postService.createPost(request, userId);
        logger.info("Post created successfully with ID: {}", response.postId());

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
