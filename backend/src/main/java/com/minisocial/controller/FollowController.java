package com.minisocial.controller;

import com.minisocial.dto.FollowRequest;
import com.minisocial.dto.FollowResponse;
import com.minisocial.security.AuthUser;
import com.minisocial.service.FollowService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for follow-related operations.
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    private static final Logger logger = LoggerFactory.getLogger(FollowController.class);

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    /**
     * Creates a follow relationship for the authenticated user.
     *
     * @param request The follow request containing target user ID
     * @return FollowResponse with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<FollowResponse> followUser(
            @Valid @RequestBody FollowRequest request,
            @AuthenticationPrincipal AuthUser follower) {

        logger.info("Follow: {} -> {}", follower.id(), request.targetUserId());

        FollowResponse response = followService.followUser(follower.id(), request.targetUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Void> unfollowUser(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal AuthUser follower) {

        logger.info("Unfollow: {} -> {}", follower.id(), targetUserId);

        followService.unfollowUser(follower.id(), targetUserId);
        return ResponseEntity.noContent().build();
    }
}
