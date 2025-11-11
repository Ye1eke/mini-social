package com.minisocial.service;

import com.minisocial.model.Post;
import com.minisocial.repository.FollowRepository;
import com.minisocial.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Asynchronous worker for rebuilding user feeds in Redis.
 * Fetches posts from followed users and stores them in a Redis sorted set.
 */
@Component
public class FeedBuilder {

    private static final Logger logger = LoggerFactory.getLogger(FeedBuilder.class);
    private static final int FEED_SIZE = 100; // Number of posts to include in feed

    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public FeedBuilder(FollowRepository followRepository, 
                      PostRepository postRepository,
                      RedisTemplate<String, Object> redisTemplate) {
        this.followRepository = followRepository;
        this.postRepository = postRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Rebuilds the feed for a user asynchronously.
     * Fetches posts from all users that the given user follows and stores them in Redis.
     * 
     * @param userId The ID of the user whose feed should be rebuilt
     */
    @Async
    public void rebuildFeed(Long userId) {
        try {
            logger.info("Starting async feed rebuild for user ID: {}", userId);

            // Get list of users that this user follows
            List<Long> followingIds = followRepository.findFollowingIdsByFollowerId(userId);

            if (followingIds.isEmpty()) {
                logger.info("User {} is not following anyone, clearing feed", userId);
                String feedKey = "feed:" + userId;
                redisTemplate.delete(feedKey);
                return;
            }

            // Fetch recent posts from followed users
            PageRequest pageRequest = PageRequest.of(0, FEED_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<Post> posts = postRepository.findByAuthorIdIn(followingIds, pageRequest);

            // Store posts in Redis sorted set with timestamp as score
            String feedKey = "feed:" + userId;
            
            // Clear existing feed
            redisTemplate.delete(feedKey);

            // Add posts to sorted set
            for (Post post : posts) {
                double score = post.getCreatedAt().getEpochSecond();
                redisTemplate.opsForZSet().add(feedKey, post.getId(), score);
            }

            logger.info("Successfully rebuilt feed for user ID: {} with {} posts", userId, posts.size());

        } catch (Exception e) {
            // Handle errors gracefully without blocking
            logger.error("Error rebuilding feed for user ID: {}. Error: {}", userId, e.getMessage(), e);
        }
    }
}
