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
        logger.info("User {} attempting to follow user {}", followerId, targetUserId);

        // Check if follow relationship already exists
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, targetUserId)) {
            logger.warn("Follow relationship already exists between {} and {}", followerId, targetUserId);
            throw new ResourceConflictException("You are already following this user");
        }

        // Check if target user exists
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with ID: " + targetUserId));

        // Check if follower exists
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("Follower user not found with ID: " + followerId));

        // Create follow relationship
        Follow follow = new Follow(follower, targetUser);
        Follow savedFollow = followRepository.save(follow);
        logger.info("Created follow relationship with ID: {}", savedFollow.getId());

        // Update follower and following counts
        follower.setFollowingCount(follower.getFollowingCount() + 1);
        targetUser.setFollowerCount(targetUser.getFollowerCount() + 1);
        userRepository.save(follower);
        userRepository.save(targetUser);
        logger.info("Updated follower counts: follower {} now following {}, target {} now has {} followers",
                followerId, follower.getFollowingCount(), targetUserId, targetUser.getFollowerCount());

        // Trigger async feed rebuild
        feedBuilder.rebuildFeed(followerId);
        logger.info("Triggered async feed rebuild for user ID: {}", followerId);

        // Return response
        return new FollowResponse(
                savedFollow.getFollower().getId(),
                savedFollow.getFollowing().getId(),
                savedFollow.getCreatedAt()
        );
    }
}
