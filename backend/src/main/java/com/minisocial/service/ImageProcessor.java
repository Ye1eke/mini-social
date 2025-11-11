package com.minisocial.service;

import com.minisocial.model.Post;
import com.minisocial.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Asynchronous worker for processing uploaded images.
 * Handles image resizing, compression, and format conversion.
 */
@Component
public class ImageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessor.class);
    private static final int MAX_WIDTH = 1200;
    private static final int MAX_HEIGHT = 1200;

    private final PostRepository postRepository;
    private final B2StorageService b2StorageService;

    public ImageProcessor(PostRepository postRepository, B2StorageService b2StorageService) {
        this.postRepository = postRepository;
        this.b2StorageService = b2StorageService;
    }

    /**
     * Processes an image asynchronously: resize, compress, and upload to B2.
     * Updates the post with the processed image URL.
     * 
     * @param postId The ID of the post to update
     * @param imageData Base64-encoded image data
     */
    @Async
    public void processImage(Long postId, String imageData) {
        try {
            logger.info("Starting async image processing for post ID: {}", postId);

            // Decode base64 image
            byte[] imageBytes = Base64.getDecoder().decode(imageData);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage originalImage = ImageIO.read(inputStream);

            if (originalImage == null) {
                logger.error("Failed to read image for post ID: {}", postId);
                return;
            }

            // Resize image if necessary
            BufferedImage processedImage = resizeImage(originalImage);

            // Compress and convert to JPEG
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(processedImage, "jpg", outputStream);
            byte[] processedBytes = outputStream.toByteArray();
            String processedBase64 = Base64.getEncoder().encodeToString(processedBytes);

            // Upload processed image to B2
            String processedImageUrl = b2StorageService.uploadFile(processedBase64, "image/jpeg");

            // Update post with processed image URL
            Post post = postRepository.findById(postId).orElse(null);
            if (post != null) {
                post.setImageUrl(processedImageUrl);
                postRepository.save(post);
                logger.info("Successfully processed and updated image for post ID: {}", postId);
            } else {
                logger.warn("Post not found for ID: {}", postId);
            }

        } catch (Exception e) {
            // Handle errors gracefully without blocking
            logger.error("Error processing image for post ID: {}. Error: {}", postId, e.getMessage(), e);
        }
    }

    /**
     * Resizes an image to fit within maximum dimensions while maintaining aspect ratio.
     * 
     * @param originalImage The original image
     * @return Resized image
     */
    private BufferedImage resizeImage(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Check if resizing is needed
        if (originalWidth <= MAX_WIDTH && originalHeight <= MAX_HEIGHT) {
            return originalImage;
        }

        // Calculate new dimensions maintaining aspect ratio
        double widthRatio = (double) MAX_WIDTH / originalWidth;
        double heightRatio = (double) MAX_HEIGHT / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        // Create resized image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();
        
        // Set rendering hints for better quality
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        graphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        graphics.dispose();

        logger.info("Resized image from {}x{} to {}x{}", originalWidth, originalHeight, newWidth, newHeight);
        return resizedImage;
    }
}
