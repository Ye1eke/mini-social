package com.minisocial.service;

import com.minisocial.dto.FollowResponse;

/**
 * Service interface for follow-related operations.
 */
public interface FollowService {

    /**
     * Creates a follow relationship between two users.
     * 
     * @param followerId The ID of the user who is following
     * @param targetUserId The ID of the user to be followed
     * @return FollowResponse containing the follow relationship details
     */
    FollowResponse followUser(Long followerId, Long targetUserId);

    void unfollowUser(Long followerId, Long targetUserId);
}
