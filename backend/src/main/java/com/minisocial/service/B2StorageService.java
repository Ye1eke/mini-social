package com.minisocial.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for uploading files to Backblaze B2 storage using AWS S3 SDK.
 * Handles image uploads with unique key generation and returns public URLs.
 */
@Service
public class B2StorageService {

    private static final Logger logger = LoggerFactory.getLogger(B2StorageService.class);

    private final AmazonS3 s3Client;
    private final String bucketName;
    private final String endpoint;

    public B2StorageService(
            @Value("${b2.endpoint}") String endpoint,
            @Value("${b2.accessKeyId}") String accessKeyId,
            @Value("${b2.secretAccessKey}") String secretAccessKey,
            @Value("${b2.bucketName}") String bucketName) {
        
        this.endpoint = endpoint;
        this.bucketName = bucketName;

        // Configure AWS S3 client for Backblaze B2
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(endpoint, "us-east-1"))
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true)
                .build();

        logger.info("B2StorageService initialized with endpoint: {}", endpoint);
    }

    /**
     * Uploads a file to Backblaze B2 storage.
     * 
     * @param imageData Base64-encoded image data
     * @param contentType MIME type of the image (e.g., "image/jpeg", "image/png")
     * @return Public URL of the uploaded file
     * @throws RuntimeException if upload fails
     */
    public String uploadFile(String imageData, String contentType) {
        try {
            // Generate unique key for the file
            String fileKey = generateUniqueKey(contentType);
            
            // Decode base64 image data
            byte[] imageBytes = Base64.getDecoder().decode(imageData);
            InputStream inputStream = new ByteArrayInputStream(imageBytes);
            
            // Set metadata
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(imageBytes.length);
            metadata.setContentType(contentType);
            
            // Upload to B2
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    fileKey,
                    inputStream,
                    metadata
            );
            
            s3Client.putObject(putObjectRequest);
            
            // Generate and return public URL
            String publicUrl = generatePublicUrl(fileKey);
            logger.info("Successfully uploaded file: {}", fileKey);
            
            return publicUrl;
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid base64 image data", e);
            throw new RuntimeException("Invalid image data format", e);
        } catch (Exception e) {
            logger.error("Failed to upload file to B2 storage", e);
            throw new RuntimeException("Failed to upload file to storage", e);
        }
    }

    /**
     * Generates a unique key for the file using UUID and file extension.
     * 
     * @param contentType MIME type to determine file extension
     * @return Unique file key
     */
    private String generateUniqueKey(String contentType) {
        String extension = getFileExtension(contentType);
        String uuid = UUID.randomUUID().toString();
        return String.format("images/%s.%s", uuid, extension);
    }

    /**
     * Extracts file extension from content type.
     * 
     * @param contentType MIME type
     * @return File extension (e.g., "jpg", "png")
     */
    private String getFileExtension(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return "jpg"; // Default extension
        }
        
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    /**
     * Generates the public URL for the uploaded file.
     * 
     * @param fileKey The key/path of the file in the bucket
     * @return Public URL
     */
    private String generatePublicUrl(String fileKey) {
        // Backblaze B2 public URL format: https://{bucketName}.{endpoint}/{fileKey}
        // Or: {endpoint}/file/{bucketName}/{fileKey}
        return String.format("%s/file/%s/%s", endpoint, bucketName, fileKey);
    }
}
