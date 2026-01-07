"use client";

import { useEffect, useState, use } from "react";
import { useRouter } from "next/navigation";
import { isAuthenticated, getToken } from "@/lib/auth";
import { followUser, unfollowUser, checkFollowStatus } from "@/lib/api";

interface UserPageProps {
  params: Promise<{
    id: string;
  }>;
}

/**
 * User profile page component for viewing other users
 * Displays user information and their posts
 */
export default function UserPage({ params }: UserPageProps) {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [isFollowing, setIsFollowing] = useState(false);
  const [followLoading, setFollowLoading] = useState(false);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const { id: userId } = use(params);
  const targetUserId = parseInt(userId);

  useEffect(() => {
    // Check if user is authenticated
    if (!isAuthenticated()) {
      router.push("/login");
      return;
    }

    // Get current user ID from token
    const token = getToken();
    if (token) {
      try {
        const payload = token.split(".")[1];
        const decodedPayload = JSON.parse(atob(payload));
        const currentId = decodedPayload.userId;
        setCurrentUserId(currentId);

        // Don't check follow status if viewing own profile
        if (currentId !== targetUserId) {
          checkFollowStatus(targetUserId)
            .then(setIsFollowing)
            .catch(console.error);
        }
      } catch (error) {
        console.error("Failed to decode JWT token:", error);
        router.push("/login");
      }
    }

    setLoading(false);
  }, [router, targetUserId]);

  const handleFollowToggle = async () => {
    if (followLoading) return;

    setFollowLoading(true);
    try {
      if (isFollowing) {
        await unfollowUser(targetUserId);
        setIsFollowing(false);
      } else {
        await followUser(targetUserId);
        setIsFollowing(true);
      }
    } catch (error) {
      console.error("Failed to toggle follow status:", error);
    } finally {
      setFollowLoading(false);
    }
  };

  const isOwnProfile = currentUserId === targetUserId;

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <div className="flex items-center justify-center py-12">
          <div className="text-lg text-gray-600">Loading...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <main className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="bg-white shadow rounded-lg">
          <div className="px-6 py-8">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-6">
                <div className="shrink-0">
                  <div className="h-20 w-20 rounded-full bg-gray-300 flex items-center justify-center">
                    <span className="text-2xl font-medium text-gray-700">
                      {userId.slice(-2)}
                    </span>
                  </div>
                </div>
                <div>
                  <h1 className="text-2xl font-bold text-gray-900">
                    User {userId}
                  </h1>
                  <p className="text-gray-600">User Profile</p>
                </div>
              </div>

              {!isOwnProfile && (
                <div className="flex flex-col items-end space-y-2">
                  <button
                    onClick={handleFollowToggle}
                    disabled={followLoading}
                    className={`px-6 py-2 rounded-md font-medium text-sm transition-colors ${
                      isFollowing
                        ? "bg-gray-200 text-gray-800 hover:bg-gray-300"
                        : "bg-blue-600 text-white hover:bg-blue-700"
                    } disabled:opacity-50 disabled:cursor-not-allowed`}
                  >
                    {followLoading
                      ? "Loading..."
                      : isFollowing
                      ? "Unfollow"
                      : "Follow"}
                  </button>
                </div>
              )}
            </div>
          </div>

          <div className="border-t border-gray-200 px-6 py-6">
            <h2 className="text-lg font-medium text-gray-900 mb-4">
              Profile Information
            </h2>
            <dl className="grid grid-cols-1 gap-x-4 gap-y-6 sm:grid-cols-2">
              <div>
                <dt className="text-sm font-medium text-gray-500">User ID</dt>
                <dd className="mt-1 text-sm text-gray-900">{userId}</dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-gray-500">
                  Member Since
                </dt>
                <dd className="mt-1 text-sm text-gray-900">Recently joined</dd>
              </div>
            </dl>
          </div>

          <div className="border-t border-gray-200 px-6 py-6">
            <h2 className="text-lg font-medium text-gray-900 mb-4">
              Recent Posts
            </h2>
            <p className="text-gray-500 text-sm">
              Posts from this user will be displayed here.
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
