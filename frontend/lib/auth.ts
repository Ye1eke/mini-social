/**
 * Authentication utility module for managing JWT tokens and user authentication state
 */

const TOKEN_KEY = "jwt_token";

/**
 * Retrieves the JWT token from localStorage
 * @returns The JWT token string or null if not found or in server-side context
 */
export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

/**
 * Stores the JWT token in localStorage
 * @param token - The JWT token to store
 */
export function setToken(token: string): void {
  if (typeof window === "undefined") return;
  localStorage.setItem(TOKEN_KEY, token);
}

/**
 * Removes the JWT token from localStorage
 */
export function clearToken(): void {
  if (typeof window === "undefined") return;
  localStorage.removeItem(TOKEN_KEY);
}

/**
 * Logs out the user by clearing the token and redirecting to login page
 */
export function logout(): void {
  clearToken();
  if (typeof window !== "undefined") {
    window.location.href = "/login";
  }
}

/**
 * Checks if the user is authenticated by verifying token existence
 * @returns True if a token exists, false otherwise
 */
export function isAuthenticated(): boolean {
  return !!getToken();
}
