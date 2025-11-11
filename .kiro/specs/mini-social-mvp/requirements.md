# Requirements Document

## Introduction

MiniSocial is a full-stack scalable social network MVP that enables users to register, authenticate, create posts with image processing, follow other users, and view personalized feeds. The system consists of a Next.js frontend deployed to Vercel and a Spring Boot backend. The backend leverages Redis for high-performance feed caching, asynchronous processing for resource-intensive operations, and Backblaze B2 for media storage.

## Glossary

- **Frontend Application**: The Next.js application providing the user interface and client-side logic
- **Backend Application**: The Spring Boot application providing REST APIs and business logic
- **Authentication Service**: The backend component responsible for user registration, login, and JWT token generation
- **Post Service**: The backend component that handles post creation and triggers asynchronous image processing
- **Follow Service**: The backend component that manages user relationships and triggers feed rebuilds
- **Feed Service**: The backend component that retrieves personalized user feeds from Redis cache
- **Image Processor**: The asynchronous worker that processes uploaded images
- **Feed Builder**: The asynchronous worker that rebuilds user feeds after follow graph changes
- **Rate Limiter**: The component that restricts authentication endpoint access to 10 requests per minute per IP
- **JWT Token**: JSON Web Token used for stateless authentication
- **Redis Sorted Set**: A Redis data structure storing feed entries with timestamp scores
- **API Client**: The frontend module that handles HTTP requests to the backend with JWT interceptors
- **Auth Guard**: The frontend component that protects routes requiring authentication

## Requirements

### Requirement 1

**User Story:** As a new user, I want to register with my email and password, so that I can create an account on MiniSocial

#### Acceptance Criteria

1. WHEN a registration request is received at POST /auth/register with valid email and password, THE Authentication Service SHALL create a new user account with BCrypt-hashed password
2. IF a registration request contains an email that already exists in the system, THEN THE Authentication Service SHALL return an error response with HTTP status 409
3. THE Authentication Service SHALL validate that the email follows standard email format before creating the account
4. THE Authentication Service SHALL validate that the password meets minimum security requirements before creating the account
5. WHEN a user account is successfully created, THE Authentication Service SHALL return a success response with HTTP status 201

### Requirement 2

**User Story:** As a registered user, I want to log in with my credentials, so that I can access my account and use the platform

#### Acceptance Criteria

1. WHEN a login request is received at POST /auth/login with valid credentials, THE Authentication Service SHALL verify the password against the stored BCrypt hash
2. WHEN credentials are successfully verified, THE Authentication Service SHALL generate a JWT token using the jjwt library
3. WHEN a JWT token is generated, THE Authentication Service SHALL return the token in the response body with HTTP status 200
4. IF login credentials are invalid, THEN THE Authentication Service SHALL return an error response with HTTP status 401
5. WHILE the Rate Limiter detects more than 10 authentication requests from a single IP within one minute, THE Authentication Service SHALL reject additional requests with HTTP status 429

### Requirement 3

**User Story:** As an authenticated user, I want to create posts with images, so that I can share content with my followers

#### Acceptance Criteria

1. WHEN a post creation request is received at POST /posts with valid JWT token and content, THE Post Service SHALL create a new post record in the database
2. WHEN a post is successfully created, THE Post Service SHALL trigger the Image Processor asynchronously using @Async annotation
3. THE Post Service SHALL upload images to Backblaze B2 storage using the AWS SDK S3 client
4. IF a post creation request lacks a valid JWT token, THEN THE Post Service SHALL return an error response with HTTP status 401
5. WHEN a post is created, THE Post Service SHALL return the post details with HTTP status 201

### Requirement 4

**User Story:** As an authenticated user, I want to follow other users, so that I can see their posts in my feed

#### Acceptance Criteria

1. WHEN a follow request is received at POST /follow with valid JWT token and target user ID, THE Follow Service SHALL create a follow relationship in the database
2. WHEN a follow relationship is successfully created, THE Follow Service SHALL trigger the Feed Builder asynchronously using @Async annotation
3. THE Follow Service SHALL update the follower count and following count for both users
4. IF a follow request targets a non-existent user, THEN THE Follow Service SHALL return an error response with HTTP status 404
5. IF a follow relationship already exists, THEN THE Follow Service SHALL return an error response with HTTP status 409

### Requirement 5

**User Story:** As an authenticated user, I want to view my personalized feed, so that I can see posts from users I follow

#### Acceptance Criteria

1. WHEN a feed request is received at GET /feed with valid JWT token, THE Feed Service SHALL retrieve posts from the Redis sorted set at key feed:{userId}
2. THE Feed Service SHALL use ZREVRANGE command to retrieve posts ordered by timestamp in descending order
3. THE Feed Service SHALL return feed entries with HTTP status 200
4. IF the Redis cache is unavailable, THEN THE Feed Service SHALL return an error response with HTTP status 503
5. THE Feed Service SHALL limit feed results to a configurable page size

### Requirement 6

**User Story:** As a system administrator, I want the platform to handle concurrent requests efficiently, so that the system remains responsive under load

#### Acceptance Criteria

1. THE Authentication Service SHALL use constructor injection for all dependencies
2. THE Post Service SHALL use record classes for all DTOs to ensure immutability
3. THE Follow Service SHALL use record classes for all DTOs to ensure immutability
4. THE Feed Service SHALL use record classes for all DTOs to ensure immutability
5. THE Rate Limiter SHALL use ConcurrentHashMap with Instant timestamps to track request counts per IP address

### Requirement 7

**User Story:** As a system administrator, I want the application to be containerized, so that it can be deployed consistently across environments

#### Acceptance Criteria

1. THE MiniSocial application SHALL provide a Dockerfile using eclipse-temurin:17-jre as the base image
2. THE Dockerfile SHALL execute the application using the command "java -jar app.jar"
3. THE application SHALL expose necessary ports for HTTP traffic
4. THE application SHALL read configuration from application.yml file
5. THE application SHALL include AsyncConfig.java for configuring the custom TaskExecutor

### Requirement 8

**User Story:** As a system administrator, I want Redis to be properly configured, so that feed caching works reliably

#### Acceptance Criteria

1. THE Backend Application SHALL include RedisConfig.java for configuring Spring Data Redis with Lettuce client
2. THE Feed Builder SHALL use ZADD command to store posts in Redis sorted sets with timestamp as score
3. THE Redis configuration SHALL define connection pool settings for optimal performance
4. THE Redis configuration SHALL set appropriate timeout values for operations
5. THE Backend Application SHALL handle Redis connection failures gracefully

### Requirement 9

**User Story:** As a user, I want to access the application through a web interface, so that I can interact with the platform easily

#### Acceptance Criteria

1. THE Frontend Application SHALL provide a login page at /login for user authentication
2. WHEN a user submits valid credentials on the login page, THE Frontend Application SHALL store the JWT token securely
3. THE Frontend Application SHALL provide a registration interface accessible from the login page
4. THE Frontend Application SHALL include an Auth Guard that redirects unauthenticated users to the login page
5. THE Frontend Application SHALL display a header with navigation and logout functionality for authenticated users

### Requirement 10

**User Story:** As an authenticated user, I want to view my feed in the web interface, so that I can see posts from users I follow

#### Acceptance Criteria

1. THE Frontend Application SHALL provide a feed page at /feed that displays posts in reverse chronological order
2. THE Frontend Application SHALL implement infinite scroll to load additional feed items as the user scrolls
3. WHEN the feed page loads, THE Frontend Application SHALL use server-side rendering to fetch initial feed data
4. THE Frontend Application SHALL display post content, author information, images, and timestamps for each feed item
5. IF the feed is empty, THEN THE Frontend Application SHALL display a helpful message to the user

### Requirement 11

**User Story:** As an authenticated user, I want to create posts through the web interface, so that I can share content with my followers

#### Acceptance Criteria

1. THE Frontend Application SHALL provide a post creation page at /post with a form for content and image upload
2. WHEN a user submits a post, THE Frontend Application SHALL send the data to POST /posts endpoint with JWT authentication
3. THE Frontend Application SHALL display a loading indicator while the post is being created
4. WHEN a post is successfully created, THE Frontend Application SHALL redirect the user to the feed page
5. IF post creation fails, THEN THE Frontend Application SHALL display an error message to the user

### Requirement 12

**User Story:** As a developer, I want the frontend to communicate with the backend securely, so that user data is protected

#### Acceptance Criteria

1. THE Frontend Application SHALL include an API client module using Axios for HTTP requests
2. THE API client SHALL automatically attach JWT tokens to all authenticated requests using interceptors
3. WHEN a request receives a 401 response, THE API client SHALL clear the stored token and redirect to login
4. THE API client SHALL set the base URL for backend API requests from environment configuration
5. THE Frontend Application SHALL store JWT tokens in httpOnly cookies or secure localStorage

### Requirement 13

**User Story:** As a developer, I want both frontend and backend to run together locally, so that I can develop and test the full stack

#### Acceptance Criteria

1. THE project SHALL include a docker-compose.yml file that starts frontend, backend, PostgreSQL, and Redis services
2. THE docker-compose.yml SHALL configure the frontend in development mode with hot reload
3. THE docker-compose.yml SHALL configure environment variables for all services
4. THE docker-compose.yml SHALL set up network connectivity between all services
5. THE docker-compose.yml SHALL expose appropriate ports for local access

### Requirement 14

**User Story:** As a developer, I want the frontend deployed to Vercel and backend to Fly.io, so that the application is accessible online

#### Acceptance Criteria

1. THE project SHALL include GitHub Actions workflow for frontend deployment to Vercel on pull requests
2. THE project SHALL include GitHub Actions workflow for backend deployment to Fly.io
3. THE frontend workflow SHALL build the Next.js application and deploy preview environments
4. THE backend workflow SHALL build the JAR file and deploy to Fly.io
5. THE deployment workflows SHALL use environment secrets for sensitive configuration
