"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { isAuthenticated } from "@/lib/auth";

/**
 * Landing page component
 * Redirects authenticated users to /feed
 * Shows welcome message and login link for unauthenticated users
 */
export default function Home() {
  const router = useRouter();

  useEffect(() => {
    // Redirect authenticated users to feed
    if (isAuthenticated()) {
      router.push("/feed");
    }
  }, [router]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50">
      <main className="mx-auto max-w-2xl px-4 py-16 text-center">
        <h1 className="mb-6 text-5xl font-bold text-gray-900">
          Welcome to MiniSocial
        </h1>
        <p className="mb-8 text-xl text-gray-600">
          Connect with others, share your thoughts, and discover amazing
          content.
        </p>
        <div className="flex flex-col items-center gap-4 sm:flex-row sm:justify-center">
          <Link
            href="/login"
            className="rounded-md bg-gray-900 px-6 py-3 text-lg font-medium text-white hover:bg-gray-700"
          >
            Get Started
          </Link>
        </div>
        <div className="mt-12 text-sm text-gray-500">
          <p>Create posts, follow users, and build your personalized feed.</p>
        </div>
      </main>
    </div>
  );
}
