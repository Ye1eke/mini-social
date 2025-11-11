"use client";

import { useEffect, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import { isAuthenticated } from "@/lib/auth";

/**
 * AuthGuard component that protects routes requiring authentication
 * Redirects unauthenticated users to the login page
 */
export default function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [isLoading, setIsLoading] = useState(true);
  const [isAuthed, setIsAuthed] = useState(false);

  useEffect(() => {
    // Public routes that don't require authentication
    const publicRoutes = ["/", "/login"];
    const isPublicRoute = publicRoutes.includes(pathname);

    // Check authentication status
    const authenticated = isAuthenticated();
    setIsAuthed(authenticated);

    // Redirect to login if not authenticated and trying to access protected route
    if (!authenticated && !isPublicRoute) {
      router.push("/login");
    } else {
      setIsLoading(false);
    }
  }, [pathname, router]);

  // Show loading state while checking authentication
  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-center">
          <div className="mb-4 inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-gray-900 border-r-transparent"></div>
          <p className="text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
