import FeedClient from "./FeedClient";
import api from "@/lib/api";
import { FeedResponse } from "@/types";
import { cookies } from "next/headers";

/**
 * Feed page - Server component that fetches initial feed data
 * Implements SSR for initial load, then delegates to client component for infinite scroll
 */
export default async function FeedPage() {
  let initialFeed: FeedResponse | null = null;
  let error: string | null = null;

  try {
    // Get JWT token from cookies for server-side request
    const cookieStore = await cookies();
    const token = cookieStore.get("jwt_token")?.value;

    // If no token in cookies, check if we can get it from localStorage (client-side only)
    // For SSR, we'll pass null and let client handle it
    if (token) {
      const response = await api.get<FeedResponse>("/feed", {
        params: { page: 0, size: 10 },
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      initialFeed = response.data;
    }
  } catch (err: any) {
    console.error("Failed to fetch initial feed:", err);
    error = err.response?.data?.message || "Failed to load feed";
  }

  return <FeedClient initialFeed={initialFeed} initialError={error} />;
}
