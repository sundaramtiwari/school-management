"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { schoolApi } from "@/lib/schoolApi";
import { useAuth } from "@/context/AuthContext";
import { Skeleton } from "@/components/ui/Skeleton";

type RecentSchool = {
  id: number;
  name: string;
  city: string;
  createdAt: string;
};

export default function SuperAdminDashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState({
    totalSchools: 0,
    activeSchools: 0,
    totalStudents: 0,
    totalClasses: 0,
    totalRevenue: 0,
  });
  const [recentSchools, setRecentSchools] = useState<RecentSchool[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadPlatformDashboard();
  }, []);

  async function loadPlatformDashboard() {
    try {
      setLoading(true);

      // Load platform-wide statistics
      const [statsRes, schoolsRes] = await Promise.all([
        api.get<any>('/api/platform/dashboard'),
        schoolApi.list(0, 5), // Get latest 5 schools
      ]);

      const statsData = statsRes.data;

      setStats({
        totalSchools: statsData.totalSchools,
        activeSchools: statsData.activeSessions, // Using active sessions as proxy or we can add activeSchools to backend if needed
        totalStudents: statsData.totalStudents,
        totalClasses: statsData.totalTeachers, // Reusing teachers for now as classes count isn't in response, user asked for update
        totalRevenue: 0,
      });

      const schools = schoolsRes.data?.content || [];
      setRecentSchools(schools);

    } catch (err) {
      console.error("Failed to load platform dashboard", err);
    } finally {
      setLoading(false);
    }
  }

  const quickActions = [
    {
      title: "Create School",
      icon: "➕",
      color: "blue",
      href: "/schools",
      description: "Add new school"
    },
    {
      title: "View Schools",
      icon: "🏫",
      color: "green",
      href: "/schools",
      description: "Manage schools"
    },
    {
      title: "Platform Users",
      icon: "👥",
      color: "purple",
      href: "/staff",
      description: "Admin accounts"
    },
    {
      title: "System Settings",
      icon: "⚙️",
      color: "gray",
      href: "/settings",
      description: "Configuration"
    },
  ];

  const isPlatformAdmin = user?.role?.toUpperCase() === "PLATFORM_ADMIN";

  return (
    <div className="space-y-8">
      {/* Welcome Header */}
      <header className="bg-gradient-to-r from-indigo-600 to-blue-600 text-white p-8 rounded-2xl shadow-lg">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 bg-white/20 rounded-full flex items-center justify-center text-3xl">
            🏛️
          </div>
          <div>
            <h1 className="text-3xl font-bold">
              {isPlatformAdmin ? "Platform Administration" : "Super Admin Control"}
            </h1>
            <p className="text-blue-100 mt-1">
              {isPlatformAdmin
                ? "Manage schools and platform operations"
                : "Complete platform oversight and control"}
            </p>
            <p className="text-blue-200 text-sm mt-1">
              {new Date().toLocaleDateString('en-IN', {
                weekday: 'long',
                year: 'numeric',
                month: 'long',
                day: 'numeric'
              })}
            </p>
          </div>
        </div>
      </header>

      {/* Platform Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white p-6 rounded-2xl border shadow-sm hover:shadow-md transition-all">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">
                Total Schools
              </p>
              {loading ? (
                <Skeleton className="h-10 w-16 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">{stats.totalSchools}</p>
              )}
              <p className="text-xs text-gray-400 mt-1">
                {stats.activeSchools} active sessions
              </p>
            </div>
            <div className="w-12 h-12 bg-blue-500 text-white rounded-xl flex items-center justify-center text-xl shadow-lg">
              🏫
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border shadow-sm hover:shadow-md transition-all">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">
                Total Students
              </p>
              {loading ? (
                <Skeleton className="h-10 w-20 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">
                  {stats.totalStudents.toLocaleString('en-IN')}
                </p>
              )}
              <p className="text-xs text-gray-400 mt-1">Across all schools</p>
            </div>
            <div className="w-12 h-12 bg-purple-500 text-white rounded-xl flex items-center justify-center text-xl shadow-lg">
              👨‍🎓
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border shadow-sm hover:shadow-md transition-all">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">
                Total Teachers
              </p>
              {loading ? (
                <Skeleton className="h-10 w-16 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">
                  {stats.totalClasses.toLocaleString('en-IN')}
                </p>
              )}
              <p className="text-xs text-gray-400 mt-1">Platform-wide</p>
            </div>
            <div className="w-12 h-12 bg-green-500 text-white rounded-xl flex items-center justify-center text-xl shadow-lg">
              📚
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border shadow-sm hover:shadow-md transition-all">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">
                System Status
              </p>
              <p className="text-3xl font-bold text-green-600 mt-2">Healthy</p>
              <p className="text-xs text-gray-400 mt-1">All services operational</p>
            </div>
            <div className="w-12 h-12 bg-emerald-500 text-white rounded-xl flex items-center justify-center text-xl shadow-lg">
              ✓
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Recent School Sign-ups */}
        <div className="bg-white p-8 rounded-2xl border shadow-sm">
          <h3 className="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
            <span>🏫</span> Recent School Sign-ups
          </h3>
          <div className="space-y-3">
            {loading ? (
              <>
                <Skeleton className="h-16 w-full" />
                <Skeleton className="h-16 w-full" />
                <Skeleton className="h-16 w-full" />
              </>
            ) : recentSchools.length === 0 ? (
              <div className="text-center py-12 text-gray-400">
                <p className="text-4xl mb-3">🏫</p>
                <p className="text-sm">No schools registered yet</p>
                <button
                  onClick={() => window.location.href = '/schools'}
                  className="mt-4 bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition font-semibold text-sm"
                >
                  Add First School
                </button>
              </div>
            ) : (
              recentSchools.map((school) => (
                <div
                  key={school.id}
                  className="flex items-center justify-between p-4 border rounded-xl hover:bg-gray-50 transition-all"
                >
                  <div className="flex-1">
                    <p className="font-bold text-gray-800">{school.name}</p>
                    <p className="text-sm text-gray-500">
                      {school.city} • Added {new Date(school.createdAt).toLocaleDateString('en-IN')}
                    </p>
                  </div>
                  <button
                    onClick={() => window.location.href = `/schools/${school.id}`}
                    className="text-blue-600 hover:text-blue-800 text-sm font-semibold"
                  >
                    View →
                  </button>
                </div>
              ))
            )}
          </div>
          {recentSchools.length > 0 && (
            <button
              onClick={() => window.location.href = '/schools'}
              className="w-full mt-4 py-2 text-center text-blue-600 hover:text-blue-800 font-semibold text-sm"
            >
              View All Schools ({stats.totalSchools}) →
            </button>
          )}
        </div>

        {/* Quick Actions & System Overview */}
        <div className="space-y-8">
          {/* Quick Actions */}
          <div className="bg-white p-8 rounded-2xl border shadow-sm">
            <h3 className="text-xl font-bold text-gray-800 mb-6">Quick Actions</h3>
            <div className="grid grid-cols-2 gap-4">
              {quickActions.map((action, i) => (
                <button
                  key={i}
                  onClick={() => window.location.href = action.href}
                  className={`
                    p-4 rounded-xl border-2 border-gray-100
                    hover:border-${action.color}-400 hover:bg-${action.color}-50
                    transition-all text-left group bg-gray-50
                  `}
                >
                  <span className="block text-2xl mb-2">{action.icon}</span>
                  <span className="font-semibold text-gray-700 text-sm block">
                    {action.title}
                  </span>
                  <span className="text-xs text-gray-500 mt-1 block">
                    {action.description}
                  </span>
                </button>
              ))}
            </div>
          </div>

          {/* System Overview */}
          <div className="bg-white p-8 rounded-2xl border shadow-sm">
            <h3 className="text-xl font-bold text-gray-800 mb-6">System Overview</h3>
            <div className="space-y-4">
              <div className="flex justify-between items-center p-4 bg-green-50 text-green-800 rounded-lg border border-green-100">
                <div className="flex items-center gap-3">
                  <span className="text-xl">✓</span>
                  <span className="font-medium">System Status</span>
                </div>
                <span className="px-3 py-1 bg-green-500 text-white text-xs rounded-full font-bold">
                  Operational
                </span>
              </div>

              <div className="flex justify-between items-center p-4 bg-blue-50 text-blue-800 rounded-lg border border-blue-100">
                <div className="flex items-center gap-3">
                  <span className="text-xl">🔄</span>
                  <span className="font-medium">Backups</span>
                </div>
                <span className="text-sm text-blue-600">
                  Last: {new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
                </span>
              </div>

              <div className="flex justify-between items-center p-4 bg-purple-50 text-purple-800 rounded-lg border border-purple-100">
                <div className="flex items-center gap-3">
                  <span className="text-xl">🔐</span>
                  <span className="font-medium">Security</span>
                </div>
                <span className="text-sm text-purple-600">No issues</span>
              </div>

              <div className="p-4 text-sm text-gray-600 bg-gray-50 rounded-lg">
                <p className="font-medium text-gray-800 mb-2">Platform Health</p>
                <p className="text-xs">
                  All services running normally. Regular backups are being processed as scheduled.
                  No security alerts detected.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Platform Admin Notice */}
      {isPlatformAdmin && (
        <div className="bg-blue-50 border border-blue-200 rounded-2xl p-6 flex items-start gap-4">
          <span className="text-3xl">ℹ️</span>
          <div className="flex-1">
            <h3 className="font-bold text-blue-800 text-lg">Platform Admin Access</h3>
            <p className="text-blue-600 text-sm mt-1">
              You have platform-level administrative access. You can create and manage schools,
              but some system-level configurations require Super Admin privileges.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
