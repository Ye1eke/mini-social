package com.minisocial.service;

import com.minisocial.dto.FeedItem;
import com.minisocial.dto.FeedResponse;
import com.minisocial.exception.ServiceUnavailableException;
import com.minisocial.model.Post;
import com.minisocial.repository.PostRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class FeedServiceImpl implements FeedService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostRepository postRepository;

    public FeedServiceImpl(RedisTemplate<String, Object> redisTemplate, PostRepository postRepository) {
        this.redisTemplate = redisTemplate;
        this.postRepository = postRepository;
    }

    @Override
    public FeedResponse getFeed(Long userId, Integer page, Integer size) {
        try {
            String feedKey = "feed:" + userId;
            
            // Calculate pagination offsets
            long start = (long) page * size;
            long end = start + size - 1;
            
            // Retrieve post IDs from Redis sorted set using ZREVRANGE (highest score first)
            Set<Object> postIds = redisTemplate.opsForZSet().reverseRange(feedKey, start, end);
            
            if (postIds == null || postIds.isEmpty()) {
                return new FeedResponse(List.of(), page, size);
            }
            
            // Convert Object set to Long list
            List<Long> postIdList = postIds.stream()
                    .map(obj -> Long.parseLong(obj.toString()))
                    .toList();
            
            // Fetch post details from database
            List<Post> posts = postRepository.findAllById(postIdList);
            
            // Convert to FeedItem DTOs
            List<FeedItem> feedItems = posts.stream()
                    .map(post -> new FeedItem(
                            post.getId(),
                            post.getAuthor().getId(),
                            post.getContent(),
                            post.getImageUrl(),
                            post.getCreatedAt()
                    ))
                    .toList();
            
            return new FeedResponse(feedItems, page, size);
            
        } catch (Exception e) {
            throw new ServiceUnavailableException("Redis cache is unavailable", e);
        }
    }
}
