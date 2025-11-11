package com.minisocial.service;

import com.minisocial.dto.CreatePostRequest;
import com.minisocial.dto.CreatePostResponse;
import com.minisocial.model.Post;
import com.minisocial.model.User;
import com.minisocial.repository.PostRepository;
import com.minisocial.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of PostService for managing post operations.
 */
@Service
public class PostServiceImpl implements PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final B2StorageService b2StorageService;
    private final ImageProcessor imageProcessor;

    public PostServiceImpl(
            PostRepository postRepository,
            UserRepository userRepository,
            B2StorageService b2StorageService,
            ImageProcessor imageProcessor) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.b2StorageService = b2StorageService;
        this.imageProcessor = imageProcessor;
    }

    @Override
    @Transactional
    public CreatePostResponse createPost(CreatePostRequest request, Long userId) {
        logger.info("Creating post for user ID: {}", userId);

        // Fetch the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Create post entity
        Post post = new Post(user, request.content(), null);

        // If image data is provided, upload to B2 immediately
        if (request.imageData() != null && !request.imageData().isEmpty()) {
            try {
                // Upload original image to B2
                String imageUrl = b2StorageService.uploadFile(request.imageData(), "image/jpeg");
                post.setImageUrl(imageUrl);
                logger.info("Uploaded original image for post");
            } catch (Exception e) {
                logger.error("Failed to upload image, creating post without image", e);
                // Continue without image if upload fails
            }
        }

        // Save post to database
        Post savedPost = postRepository.save(post);
        logger.info("Post created with ID: {}", savedPost.getId());

        // Trigger async image processing if image data is provided
        if (request.imageData() != null && !request.imageData().isEmpty()) {
            imageProcessor.processImage(savedPost.getId(), request.imageData());
            logger.info("Triggered async image processing for post ID: {}", savedPost.getId());
        }

        // Return response
        return new CreatePostResponse(
                savedPost.getId(),
                savedPost.getAuthor().getId(),
                savedPost.getContent(),
                savedPost.getImageUrl(),
                savedPost.getCreatedAt()
        );
    }
}
