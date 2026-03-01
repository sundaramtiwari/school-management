"use client";

import { useAuth } from "@/context/AuthContext";
import SchoolAdminDashboard from "@/components/dashboards/SchoolAdminDashboard";
import TeacherDashboard from "@/components/dashboards/TeacherDashboard";
import AccountantDashboard from "@/components/dashboards/AccountantDashboard";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

/**
 * Main Dashboard Page with Role-Based Routing
 * 
 * This component automatically displays the correct dashboard
 * based on the logged-in user's role.
 */
export default function DashboardPage() {
  const { user, isLoading } = useAuth();
  const router = useRouter();

  // Show loading state while auth is initializing
  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen mx-auto px-6 py-6">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-500 text-base">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  // Ensure user is logged in
  if (!user) {
    return (
      <div className="flex items-center justify-center min-h-screen mx-auto px-6 py-6">
        <div className="text-center">
          <p className="text-gray-500 text-base">Please log in to view dashboard</p>
        </div>
      </div>
    );
  }

  const role = user.role?.toUpperCase();

  switch (role) {
    case "SUPER_ADMIN":
    case "PLATFORM_ADMIN":
      router.push("/platform/dashboard");
      return null;

    case "SCHOOL_ADMIN":
      return <SchoolAdminDashboard />;

    case "TEACHER":
      return <TeacherDashboard />;

    case "ACCOUNTANT":
      return <AccountantDashboard />;

    case "PARENT":
      return (
        <div className="text-center py-20 mx-auto px-6">
          <h1 className="text-lg font-semibold mb-4">Parent Portal</h1>
          <p className="text-gray-500 text-base">Coming soon!</p>
        </div>
      );

    default:
      return (
        <div className="text-center py-20 mx-auto px-6">
          <h1 className="text-lg font-semibold mb-4">Welcome!</h1>
          <p className="text-gray-500 text-base">
            Your role ({user.role}) does not have a dashboard configured yet.
          </p>
        </div>
      );
  }
}
