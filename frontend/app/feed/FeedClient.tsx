"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { useRouter } from "next/navigation";
import PostCard from "@/components/PostCard";
import api from "@/lib/api";
import { FeedResponse, FeedItem } from "@/types";
import { isAuthenticated } from "@/lib/auth";

interface FeedClientProps {
  initialFeed: FeedResponse | null;
  initialError: string | null;
}

/**
 * FeedClient component handles client-side infinite scroll and feed loading
 */
export default function FeedClient({
  initialFeed,
  initialError,
}: FeedClientProps) {
  const router = useRouter();
  const [posts, setPosts] = useState<FeedItem[]>(initialFeed?.items || []);
  const [page, setPage] = useState(initialFeed ? 1 : 0);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState<string | null>(initialError);
  const observerTarget = useRef<HTMLDivElement>(null);

  // Check authentication on mount
  useEffect(() => {
    if (!isAuthenticated()) {
      router.push("/login");
    }
  }, [router]);

  // Fetch initial feed if not provided by SSR
  useEffect(() => {
    if (!initialFeed && !initialError && isAuthenticated()) {
      fetchFeed(0);
    }
  }, [initialFeed, initialError]);

  /**
   * Fetch feed data from API
   */
  const fetchFeed = async (pageNum: number) => {
    if (loading) return;

    setLoading(true);
    setError(null);

    try {
      const response = await api.get<FeedResponse>("/feed", {
        params: {
          page: pageNum,
          size: 10,
        },
      });

      const newPosts = response.data.items;

      if (newPosts.length === 0) {
        setHasMore(false);
      } else {
        setPosts((prev) => {
          // Avoid duplicates
          const existingIds = new Set(prev.map((p) => p.postId));
          const uniqueNewPosts = newPosts.filter(
            (p) => !existingIds.has(p.postId)
          );
          return [...prev, ...uniqueNewPosts];
        });
        setPage(pageNum + 1);

        // If we got fewer posts than requested, we've reached the end
        if (newPosts.length < 10) {
          setHasMore(false);
        }
      }
    } catch (err: any) {
      console.error("Failed to fetch feed:", err);
      setError(err.response?.data?.message || "Failed to load feed");
    } finally {
      setLoading(false);
    }
  };

  /**
   * Load more posts when user scrolls to bottom
   */
  const loadMore = useCallback(() => {
    if (hasMore && !loading) {
      fetchFeed(page);
    }
  }, [page, hasMore, loading]);

  /**
   * Set up Intersection Observer for infinite scroll
   */
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          loadMore();
        }
      },
      { threshold: 0.1 }
    );

    const currentTarget = observerTarget.current;
    if (currentTarget) {
      observer.observe(currentTarget);
    }

    return () => {
      if (currentTarget) {
        observer.unobserve(currentTarget);
      }
    };
  }, [loadMore]);

  // Error state
  if (error && posts.length === 0) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <div className="rounded-lg border border-red-200 bg-red-50 p-6 text-center">
          <p className="text-red-800">{error}</p>
          <button
            onClick={() => fetchFeed(0)}
            className="mt-4 rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  // Empty state
  if (!loading && posts.length === 0) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <div className="rounded-lg border border-gray-200 bg-white p-12 text-center">
          <svg
            className="mx-auto h-12 w-12 text-gray-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z"
            />
          </svg>
          <h3 className="mt-4 text-lg font-medium text-gray-900">
            No posts yet
          </h3>
          <p className="mt-2 text-sm text-gray-500">
            Follow some users or create your first post to get started!
          </p>
          <button
            onClick={() => router.push("/post")}
            className="mt-6 rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700"
          >
            Create Post
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold text-gray-200">Your Feed</h1>

      {/* Posts List */}
      <div className="space-y-6">
        {posts.map((post) => (
          <PostCard key={post.postId} post={post} />
        ))}
      </div>

      {/* Loading Indicator */}
      {loading && (
        <div className="mt-6 flex justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-300 border-t-gray-900"></div>
        </div>
      )}

      {/* Error during pagination */}
      {error && posts.length > 0 && (
        <div className="mt-6 rounded-lg border border-red-200 bg-red-50 p-4 text-center">
          <p className="text-sm text-red-800">{error}</p>
        </div>
      )}

      {/* End of feed message */}
      {!hasMore && posts.length > 0 && (
        <div className="mt-6 text-center">
          <p className="text-sm text-gray-500">You&apos;ve reached the end!</p>
        </div>
      )}

      {/* Intersection Observer Target */}
      <div ref={observerTarget} className="h-4" />
    </div>
  );
}
