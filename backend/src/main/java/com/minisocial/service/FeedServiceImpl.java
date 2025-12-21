package com.minisocial.service;

import com.minisocial.dto.FeedItem;
import com.minisocial.dto.FeedResponse;
import com.minisocial.model.Post;
import com.minisocial.repository.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedServiceImpl implements FeedService {

    private final PostRepository postRepository;

    public FeedServiceImpl(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public FeedResponse getFeed(Long userId, Integer page, Integer size) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0) ? 20 : Math.min(size, 100);

        Pageable pageable = PageRequest.of(p, s);

        List<Post> posts = postRepository.findFeedForUser(userId, pageable);

        List<FeedItem> items = posts.stream()
                .map(post -> new FeedItem(
                        post.getId(),
                        post.getAuthor().getId(),
                        post.getContent(),
                        post.getImageUrl(),
                        post.getCreatedAt()
                ))
                .toList();

        return new FeedResponse(items, p, s);
    }
}
