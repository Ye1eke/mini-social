# Implementation Plan

## Backend Implementation

- [x] 1. Set up Spring Boot project structure and core configuration

  - Create Maven project with Spring Boot 3.3 parent
  - Add dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-data-redis, spring-boot-starter-security, postgresql, lettuce-core, jjwt, aws-java-sdk-s3, spring-boot-starter-validation
  - Create main application class MiniSocialApplication.java with @SpringBootApplication and @EnableAsync
  - Create application.yml with datasource, Redis, JWT, B2, and async configuration
  - _Requirements: 6.1, 7.4, 7.5, 8.1_

- [x] 2. Implement configuration classes

  - [x] 2.1 Create AsyncConfig.java with custom TaskExecutor bean

    - Configure core pool size, max pool size, queue capacity
    - Set thread name prefix for debugging
    - _Requirements: 7.5, 6.1_

  - [x] 2.2 Create RedisConfig.java with RedisTemplate configuration
    - Configure Lettuce connection factory
    - Set up String key serializer and JSON value serializer
    - Configure connection pool settings
    - _Requirements: 8.1, 8.3, 8.4, 8.5_

- [x] 3. Implement User entity and repository

  - [x] 3.1 Create User.java entity class

    - Add fields: id, email, passwordHash, followerCount, followingCount, createdAt
    - Add JPA annotations and constraints
    - _Requirements: 1.1, 2.1_

  - [x] 3.2 Create UserRepository interface
    - Extend JpaRepository<User, Long>
    - Add findByEmail and existsByEmail methods
    - _Requirements: 1.1, 2.1_

- [x] 4. Implement JWT security infrastructure

  - [x] 4.1 Create JwtUtil.java utility class

    - Implement generateToken method with user claims
    - Implement validateToken method
    - Implement extractUsername method
    - Use jjwt library for token operations
    - _Requirements: 2.2, 2.3_

  - [x] 4.2 Create JwtAuthenticationFilter.java

    - Extend OncePerRequestFilter
    - Extract and validate JWT from Authorization header
    - Set authentication in SecurityContext
    - _Requirements: 2.2, 3.4_

  - [x] 4.3 Create SecurityConfig.java
    - Configure Spring Security filter chain
    - Disable CSRF for stateless API
    - Define public endpoints (/auth/register, /auth/login)
    - Add JWT filter to security chain
    - Configure BCrypt password encoder bean
    - _Requirements: 2.1, 2.2, 3.4_

- [x] 5. Implement rate limiting

  - [x] 5.1 Create RateLimiter.java component
    - Use ConcurrentHashMap<String, List<Instant>> for tracking
    - Implement allowRequest method that checks IP request count
    - Remove expired entries older than 1 minute
    - Configure for 10 requests per minute
    - _Requirements: 2.5, 6.5_

- [x] 6. Implement authentication module

  - [x] 6.1 Create authentication DTOs as record classes

    - RegisterRequest(String email, String password)
    - RegisterResponse(Long userId, String email)
    - LoginRequest(String email, String password)
    - LoginResponse(String token, Long userId, String email)
    - Add validation annotations
    - _Requirements: 1.1, 2.1, 6.2_

  - [x] 6.2 Create AuthService interface and AuthServiceImpl

    - Implement register method with BCrypt hashing
    - Implement login method with password verification and JWT generation
    - Check for duplicate email on registration
    - Use constructor injection for dependencies
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 6.1_

  - [x] 6.3 Create AuthController.java
    - Implement POST /auth/register endpoint
    - Implement POST /auth/login endpoint
    - Integrate RateLimiter for both endpoints
    - Return appropriate HTTP status codes
    - Extract client IP from request
    - _Requirements: 1.1, 1.2, 1.5, 2.3, 2.4, 2.5_

- [x] 7. Implement Post entity and repository

  - [x] 7.1 Create Post.java entity class

    - Add fields: id, author (User), content, imageUrl, createdAt
    - Add JPA annotations and relationships
    - _Requirements: 3.1_

  - [x] 7.2 Create PostRepository interface
    - Extend JpaRepository<Post, Long>
    - Add findByAuthorIdIn method with Pageable
    - _Requirements: 3.1, 5.1_

- [x] 8. Implement Backblaze B2 storage service

  - [x] 8.1 Create B2StorageService.java
    - Configure AWS S3 client for Backblaze B2
    - Implement uploadFile method with unique key generation
    - Return public URL after upload
    - Handle upload failures with proper exceptions
    - _Requirements: 3.3_

- [x] 9. Implement post creation with async image processing

  - [x] 9.1 Create post DTOs as record classes

    - CreatePostRequest(String content, String imageData)
    - CreatePostResponse(Long postId, Long userId, String content, String imageUrl, Instant createdAt)
    - Add validation annotations
    - _Requirements: 3.1, 3.5, 6.2, 6.3_

  - [x] 9.2 Create ImageProcessor.java async worker

    - Annotate with @Async
    - Implement processImage method for resize/compress
    - Update post with processed image URL
    - Handle errors gracefully without blocking
    - _Requirements: 3.2_

  - [x] 9.3 Create PostService interface and PostServiceImpl

    - Implement createPost method
    - Save post to database
    - Upload image to B2 if provided
    - Trigger ImageProcessor asynchronously
    - Use constructor injection
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 6.1_

  - [x] 9.4 Create PostController.java
    - Implement POST /posts endpoint
    - Extract user ID from JWT authentication
    - Validate JWT token
    - Return 201 on success
    - _Requirements: 3.1, 3.4, 3.5_

- [x] 10. Implement Follow entity and repository

  - [x] 10.1 Create Follow.java entity class

    - Add fields: id, follower (User), following (User), createdAt
    - Add unique constraint on follower_id and following_id
    - Add JPA annotations
    - _Requirements: 4.1_

  - [x] 10.2 Create FollowRepository interface
    - Extend JpaRepository<Follow, Long>
    - Add existsByFollowerIdAndFollowingId method
    - Add findFollowingIdsByFollowerId method
    - _Requirements: 4.1, 4.5_

- [x] 11. Implement follow functionality with async feed rebuild

  - [x] 11.1 Create follow DTOs as record classes

    - FollowRequest(Long targetUserId)
    - FollowResponse(Long followerId, Long followingId, Instant createdAt)
    - Add validation annotations
    - _Requirements: 4.1, 6.2, 6.3_

  - [x] 11.2 Create FeedBuilder.java async worker

    - Annotate with @Async
    - Implement rebuildFeed method
    - Fetch posts from followed users
    - Use Redis ZADD to store in sorted set with key feed:{userId}
    - Use timestamp as score
    - _Requirements: 4.2, 8.2_

  - [x] 11.3 Create FollowService interface and FollowServiceImpl

    - Implement followUser method
    - Check if follow relationship already exists
    - Check if target user exists
    - Update follower and following counts
    - Trigger FeedBuilder asynchronously
    - Use constructor injection
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 6.1_

  - [x] 11.4 Create FollowController.java
    - Implement POST /follow endpoint
    - Extract follower ID from JWT authentication
    - Validate JWT token
    - Return appropriate status codes
    - _Requirements: 4.1, 4.4, 4.5_

- [x] 12. Implement feed retrieval from Redis

  - [x] 12.1 Create feed DTOs as record classes

    - FeedResponse(List<FeedItem> items, Integer page, Integer size)
    - FeedItem(Long postId, Long authorId, String content, String imageUrl, Instant createdAt)
    - _Requirements: 5.3, 6.2, 6.3_

  - [x] 12.2 Create FeedService interface and FeedServiceImpl

    - Implement getFeed method with pagination
    - Use RedisTemplate to execute ZREVRANGE command
    - Retrieve posts from Redis sorted set feed:{userId}
    - Fetch post details from PostRepository
    - Handle Redis unavailability with ServiceUnavailableException
    - Use constructor injection
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 8.5_

  - [x] 12.3 Create FeedController.java
    - Implement GET /feed endpoint with pagination parameters
    - Extract user ID from JWT authentication
    - Validate JWT token
    - Return 200 with feed data
    - _Requirements: 5.1, 5.3_

- [x] 13. Implement global exception handling

  - [x] 13.1 Create custom exception classes

    - InvalidRequestException with @ResponseStatus(400)
    - UnauthorizedException with @ResponseStatus(401)
    - ResourceNotFoundException with @ResponseStatus(404)
    - ResourceConflictException with @ResponseStatus(409)
    - RateLimitExceededException with @ResponseStatus(429)
    - ServiceUnavailableException with @ResponseStatus(503)
    - _Requirements: 1.2, 2.4, 2.5, 4.4, 4.5, 5.4_

  - [x] 13.2 Create ErrorResponse record class

    - Fields: String error, String message, Instant timestamp
    - _Requirements: 6.2_

  - [x] 13.3 Create GlobalExceptionHandler.java
    - Annotate with @RestControllerAdvice
    - Add exception handler methods for all custom exceptions
    - Return ErrorResponse with appropriate status codes
    - Log errors appropriately
    - _Requirements: 1.2, 2.4, 2.5, 4.4, 4.5, 5.4_

- [x] 14. Create backend Dockerfile
  - Create Dockerfile using eclipse-temurin:17-jre base image
  - Copy JAR file to /app directory
  - Expose port 8080
  - Set entrypoint to "java -jar app.jar"
  - _Requirements: 7.1, 7.2, 7.3_

## Frontend Implementation

- [x] 15. Set up Next.js project structure

  - Initialize Next.js 14+ project with TypeScript
  - Install dependencies: axios, tailwindcss
  - Configure tailwind.config.js and globals.css
  - Create folder structure: app/, lib/, components/, types/, public/
  - Create next.config.js with image domains and environment variables
  - _Requirements: 9.1, 12.4_

- [x] 16. Implement authentication utilities

  - [x] 16.1 Create lib/auth.ts module

    - Implement getToken function to retrieve JWT from localStorage
    - Implement setToken function to store JWT
    - Implement clearToken function to remove JWT
    - Implement logout function to clear token and redirect
    - Implement isAuthenticated function
    - _Requirements: 9.2, 12.2, 12.5_

  - [x] 16.2 Create lib/api.ts API client
    - Create Axios instance with base URL from environment
    - Add request interceptor to attach JWT token
    - Add response interceptor to handle 401 errors
    - Clear token and redirect to login on 401
    - _Requirements: 12.1, 12.2, 12.3, 12.4_

- [x] 17. Create TypeScript type definitions

  - Create types/index.ts with interfaces
  - Define User, Post, FeedItem, LoginRequest, LoginResponse, RegisterRequest, CreatePostRequest types
  - _Requirements: 9.1, 10.4, 11.2_

- [x] 18. Implement authentication pages

  - [x] 18.1 Create app/login/page.tsx
    - Create login form with email and password fields
    - Create registration form (toggle or tabs)
    - Call api.post('/auth/login') with credentials
    - Call api.post('/auth/register') for registration
    - Store JWT token using setToken on success
    - Redirect to /feed after authentication
    - Display error messages on failure
    - _Requirements: 9.1, 9.2, 9.3, 11.5_

- [x] 19. Implement layout and navigation

  - [x] 19.1 Create components/Header.tsx

    - Display logo and navigation links
    - Show links to /feed and /post
    - Display user email from JWT token
    - Add logout button that calls logout()
    - Make responsive
    - _Requirements: 9.5_

  - [x] 19.2 Create components/AuthGuard.tsx

    - Check authentication status using isAuthenticated()
    - Redirect to /login if not authenticated
    - Show loading state during check
    - _Requirements: 9.4_

  - [x] 19.3 Create app/layout.tsx

    - Set up root layout with metadata
    - Include Header component
    - Wrap children with AuthGuard for protected routes
    - Apply global styles
    - _Requirements: 9.4, 9.5_

  - [x] 19.4 Create app/page.tsx
    - Create landing page
    - Redirect authenticated users to /feed
    - Show welcome message and login link for unauthenticated users
    - _Requirements: 9.1_

- [x] 20. Implement feed page with infinite scroll

  - [x] 20.1 Create components/PostCard.tsx

    - Display post content, author info, image, and timestamp
    - Format timestamp as relative time
    - Handle responsive image display
    - _Requirements: 10.4_

  - [x] 20.2 Create app/feed/page.tsx
    - Implement as async server component for initial SSR
    - Fetch initial feed data from GET /feed
    - Implement client-side infinite scroll
    - Load more posts as user scrolls
    - Display PostCard components for each item
    - Show empty state when no posts
    - Handle loading and error states
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [x] 21. Implement post creation page
  - [x] 21.1 Create app/post/page.tsx
    - Create form with textarea for content
    - Add file input for image upload
    - Convert image to base64 or FormData
    - Call api.post('/posts') with data
    - Show loading indicator during submission
    - Redirect to /feed on success
    - Display error message on failure
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

## Infrastructure and Deployment

- [x] 22. Create Docker Compose configuration

  - Create docker-compose.yml with services: frontend, backend, postgres, redis
  - Configure frontend in development mode with volume mounts
  - Configure backend with environment variables
  - Set up PostgreSQL with initialization
  - Set up Redis with persistence
  - Configure network connectivity between services
  - Expose appropriate ports
  - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

- [ ] 23. Create GitHub Actions workflows

  - [ ] 23.1 Create .github/workflows/frontend.yml

    - Set up Node.js environment
    - Install Vercel CLI
    - Deploy to Vercel on push to main
    - Create preview deployments for pull requests
    - Use GitHub secrets for tokens
    - _Requirements: 14.1, 14.3_

  - [ ] 23.2 Create .github/workflows/backend.yml
    - Set up Java 17 environment
    - Build JAR with Maven
    - Install Fly.io CLI
    - Deploy to Fly.io on push to main
    - Use GitHub secrets for API tokens
    - _Requirements: 14.2, 14.4, 14.5_

- [ ] 24. Create project documentation
  - Create README.md with architecture diagram
  - Document local development setup
  - Document deployment process
  - Include environment variable configuration
  - Add API endpoint documentation
  - _Requirements: 13.1_
