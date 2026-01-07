/**
 * API client module for making authenticated HTTP requests to the backend
 */

import axios from "axios";
import { getToken, clearToken } from "./auth";

/**
 * Axios instance configured with base URL and interceptors
 */
const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
  },
});

export const authApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
  },
});

/**
 * Request interceptor - automatically attaches JWT token to all requests
 */
api.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Response interceptor - handles 401 errors by clearing token and redirecting to login
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearToken();
      if (
        typeof window !== "undefined" &&
        !window.location.pathname.includes("/login")
      ) {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

export default api;
/**
 * Follow a user
 */
export const followUser = async (targetUserId: number): Promise<void> => {
  await api.post("/follow", { targetUserId });
};

/**
 * Unfollow a user
 */
export const unfollowUser = async (targetUserId: number): Promise<void> => {
  await api.delete(`/follow/${targetUserId}`);
};

/**
 * Check if current user is following a specific user
 */
export const checkFollowStatus = async (
  targetUserId: number
): Promise<boolean> => {
  try {
    const response = await api.get(`/follow/status/${targetUserId}`);
    return response.data.isFollowing;
  } catch (error) {
    // If endpoint doesn't exist, we'll handle this gracefully
    console.warn("Follow status check not available");
    return false;
  }
};
