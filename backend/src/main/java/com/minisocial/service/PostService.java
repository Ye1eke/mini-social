package com.minisocial.service;

import com.minisocial.dto.CreatePostRequest;
import com.minisocial.dto.CreatePostResponse;

/**
 * Service interface for post-related operations.
 */
public interface PostService {

    /**
     * Creates a new post for the specified user.
     * 
     * @param request The post creation request containing content and optional image data
     * @param userId The ID of the user creating the post
     * @return CreatePostResponse containing the created post details
     */
    CreatePostResponse createPost(CreatePostRequest request, Long userId);
}
