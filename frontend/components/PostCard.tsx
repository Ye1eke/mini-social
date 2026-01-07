"use client";

import Image from "next/image";
import Link from "next/link";
import { FeedItem } from "@/types";

interface PostCardProps {
  post: FeedItem;
}

/**
 * PostCard component displays a single post with content, author info, image, and timestamp
 * Formats timestamp as relative time (e.g., "2 hours ago")
 */
export default function PostCard({ post }: PostCardProps) {
  /**
   * Format timestamp as relative time
   */
  const formatRelativeTime = (timestamp: string): string => {
    const now = new Date();
    const postDate = new Date(timestamp);
    const diffInSeconds = Math.floor(
      (now.getTime() - postDate.getTime()) / 1000
    );

    if (diffInSeconds < 60) {
      return "just now";
    }

    const diffInMinutes = Math.floor(diffInSeconds / 60);
    if (diffInMinutes < 60) {
      return `${diffInMinutes} ${
        diffInMinutes === 1 ? "minute" : "minutes"
      } ago`;
    }

    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) {
      return `${diffInHours} ${diffInHours === 1 ? "hour" : "hours"} ago`;
    }

    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays < 30) {
      return `${diffInDays} ${diffInDays === 1 ? "day" : "days"} ago`;
    }

    const diffInMonths = Math.floor(diffInDays / 30);
    if (diffInMonths < 12) {
      return `${diffInMonths} ${diffInMonths === 1 ? "month" : "months"} ago`;
    }

    const diffInYears = Math.floor(diffInMonths / 12);
    return `${diffInYears} ${diffInYears === 1 ? "year" : "years"} ago`;
  };

  return (
    <article className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-md">
      {/* Author Info */}
      <div className="mb-4 flex items-center">
        <Link
          href={`/user/${post.authorId}`}
          className="flex items-center hover:opacity-80 transition-opacity"
        >
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gray-300 text-sm font-semibold text-gray-700">
            {post.authorId.toString().slice(-2)}
          </div>
          <div className="ml-3">
            <p className="text-sm font-medium text-gray-900 hover:underline">
              User {post.authorId}
            </p>
            <p className="text-xs text-gray-500">
              {formatRelativeTime(post.createdAt)}
            </p>
          </div>
        </Link>
      </div>

      {/* Post Content */}
      {post.content && (
        <p className="mb-4 whitespace-pre-wrap text-gray-800">{post.content}</p>
      )}

      {/* Post Image */}
      {post.imageUrl && (
        <div className="relative aspect-video w-full overflow-hidden rounded-lg bg-gray-100">
          <Image
            src={post.imageUrl}
            alt="Post image"
            fill
            className="object-cover"
            sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
          />
        </div>
      )}
    </article>
  );
}
