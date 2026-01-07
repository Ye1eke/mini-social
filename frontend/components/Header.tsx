"use client";

import Link from "next/link";
import { logout, getToken, isAuthenticated } from "@/lib/auth";
import { useEffect, useState } from "react";

/**
 * Header component with navigation and user info
 * Displays logo, navigation links, user email, and logout button
 */
export default function Header() {
  const [userEmail, setUserEmail] = useState<string | null>(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  useEffect(() => {
    const checkAuthStatus = () => {
      const authenticated = isAuthenticated();
      setIsLoggedIn(authenticated);

      if (authenticated) {
        const token = getToken();
        if (token) {
          try {
            const payload = token.split(".")[1];
            const decodedPayload = JSON.parse(atob(payload));
            setUserEmail(decodedPayload.sub || decodedPayload.email || null);
          } catch (error) {
            console.error("Failed to decode JWT token:", error);
          }
        }
      } else {
        setUserEmail(null);
      }
    };

    checkAuthStatus();

    const interval = setInterval(checkAuthStatus, 1000);

    return () => {
      clearInterval(interval);
    };
  }, []);

  const handleLogout = () => {
    logout();
  };

  return (
    <header className="border-b border-gray-200 bg-white">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          {/* Logo */}
          <div className="flex items-center">
            <Link href="/feed" className="text-xl font-bold text-gray-900">
              MiniSocial
            </Link>
          </div>

          {isLoggedIn && (
            <nav className="flex items-center space-x-4">
              <Link
                href="/feed"
                className="rounded-md px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 hover:text-gray-900"
              >
                Feed
              </Link>
              <Link
                href="/post"
                className="rounded-md px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 hover:text-gray-900"
              >
                Create Post
              </Link>
            </nav>
          )}

          {isLoggedIn && (
            <div className="flex items-center space-x-4">
              {userEmail && (
                <Link
                  href="/profile"
                  className="hidden text-sm text-gray-600 hover:text-gray-900 hover:underline sm:inline"
                >
                  {userEmail}
                </Link>
              )}
              <button
                onClick={handleLogout}
                className="rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700"
              >
                Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
