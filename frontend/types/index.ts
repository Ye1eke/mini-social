export interface User {
  id: number;
  email: string;
}

export interface Post {
  postId: number;
  userId: number;
  content: string;
  imageUrl?: string;
  createdAt: string;
}

export interface FeedItem {
  postId: number;
  authorId: number;
  content: string;
  imageUrl?: string;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: number;
  email: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

export interface RegisterResponse {
  userId: number;
  email: string;
}

export interface CreatePostRequest {
  content: string;
  imageData?: string;
}

export interface CreatePostResponse {
  postId: number;
  userId: number;
  content: string;
  imageUrl?: string;
  createdAt: string;
}

export interface FeedResponse {
  items: FeedItem[];
  page: number;
  size: number;
}

export interface FollowRequest {
  targetUserId: number;
}

export interface FollowResponse {
  followerId: number;
  followingId: number;
  createdAt: string;
}

export interface ErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}
