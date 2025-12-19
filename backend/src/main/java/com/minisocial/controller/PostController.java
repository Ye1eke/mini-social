package com.minisocial.controller;

import com.minisocial.dto.CreatePostRequest;
import com.minisocial.dto.CreatePostResponse;
import com.minisocial.security.AuthUser;
import com.minisocial.service.PostService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * Creates a new post for the authenticated user.
     * 
     * @param request The post creation request
     * @return CreatePostResponse with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<CreatePostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal AuthUser user) {

        logger.info("Received post creation request");

        CreatePostResponse response = postService.createPost(request, user.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
