package com.minisocial.service;

import com.minisocial.dto.FollowResponse;
import com.minisocial.exception.ResourceConflictException;
import com.minisocial.exception.ResourceNotFoundException;
import com.minisocial.model.Follow;
import com.minisocial.model.User;
import com.minisocial.repository.FollowRepository;
import com.minisocial.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of FollowService for managing follow relationships.
 */
@Service
public class FollowServiceImpl implements FollowService {

    private static final Logger logger = LoggerFactory.getLogger(FollowServiceImpl.class);

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FeedBuilder feedBuilder;

    public FollowServiceImpl(
            FollowRepository followRepository,
            UserRepository userRepository,
            FeedBuilder feedBuilder) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.feedBuilder = feedBuilder;
    }

    @Override
    @Transactional
    public FollowResponse followUser(Long followerId, Long targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new IllegalArgumentException("You can't follow yourself");
        }

        // Check users exist
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with ID: " + targetUserId));
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("Follower user not found with ID: " + followerId));

        // Fast pre-check (still keep DB constraint for race safety)
        if (followRepository.existsByFollower_IdAndFollowing_Id(followerId, targetUserId)) {
            throw new ResourceConflictException("You are already following this user");
        }

        try {
            Follow saved = followRepository.save(new Follow(follower, targetUser));

            userRepository.incFollowing(followerId);
            userRepository.incFollowers(targetUserId);

            feedBuilder.rebuildFeed(followerId);

            return new FollowResponse(
                    followerId,
                    targetUserId,
                    saved.getCreatedAt()
            );
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // if 2 requests hit at the same time, unique constraint may trigger here
            throw new ResourceConflictException("You are already following this user");
        }
    }

    @Override
    @Transactional
    public void unfollowUser(Long followerId, Long targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new IllegalArgumentException("You can't unfollow yourself");
        }

        // delete row
        int deleted = followRepository.deleteByFollowerIdAndFollowingId(followerId, targetUserId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Follow relationship not found");
        }

        userRepository.decFollowing(followerId);
        userRepository.decFollowers(targetUserId);

        feedBuilder.rebuildFeed(followerId);
    }
}
