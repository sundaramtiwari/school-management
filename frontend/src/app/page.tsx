"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { schoolApi } from "@/lib/schoolApi";
import { useAuth } from "@/context/AuthContext";
import { Skeleton } from "@/components/ui/Skeleton";

export default function DashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState({
    schools: 0,
    students: 0,
    classes: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return; // Wait for user info

    async function loadStats() {
      try {
        setLoading(true);
        const currentRole = user?.role?.toUpperCase();
        const isHighLevelAdmin = currentRole === "SUPER_ADMIN" || currentRole === "PLATFORM_ADMIN";

        // Fetching some basic counts to show on dashboard
        const [schoolsRes, classesRes, studentsRes] = await Promise.all([
          isHighLevelAdmin ? schoolApi.list(0, 1) : Promise.resolve({ data: { totalElements: 1 } }),
          api.get(`/api/classes${isHighLevelAdmin ? "" : "/mine"}?size=1`),
          api.get(`/api/students${isHighLevelAdmin ? "" : "/mine"}?size=1`)
        ]);

        setStats({
          schools: schoolsRes.data?.totalElements || 0,
          classes: classesRes.data?.totalElements || 0,
          students: studentsRes.data?.totalElements || 0,
        });
      } catch (err) {
        console.error("Failed to load dashboard stats", err);
      } finally {
        setLoading(false);
      }
    }
    loadStats();
  }, [user]);

  const currentRole = user?.role?.toUpperCase();
  const isHighLevelAdmin = currentRole === "SUPER_ADMIN" || currentRole === "PLATFORM_ADMIN";

  const cards = [
    { label: "Total Schools", value: stats.schools, color: "bg-blue-500", icon: "🏫", hide: !isHighLevelAdmin },
    { label: "Total Classes", value: stats.classes, color: "bg-green-500", icon: "📚" },
    { label: "Total Students", value: stats.students, color: "bg-purple-500", icon: "🎓" },
  ];

  return (
    <div className="space-y-8">
      <header>
        <h1 className="text-3xl font-bold text-gray-800">Welcome Back, {user?.role?.replace('_', ' ') || "User"}!</h1>
        <p className="text-gray-500 mt-2">Here's what's happening in your school system today.</p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {cards.map((card, i) => !card.hide && (
          <div key={i} className="bg-white p-6 rounded-2xl border shadow-sm flex items-center gap-4 transition-transform hover:scale-[1.02]">
            <div className={`w-14 h-14 ${card.color} text-white rounded-xl flex items-center justify-center text-2xl shadow-lg`}>
              {card.icon}
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">{card.label}</p>
              {loading ? (
                <Skeleton className="h-8 w-16 mt-1" />
              ) : (
                <p className="text-3xl font-bold text-gray-900">{card.value}</p>
              )}
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Placeholder for Recent Activity or Shortcuts */}
        <div className="bg-white p-8 rounded-2xl border shadow-sm space-y-4">
          <h3 className="text-xl font-bold text-gray-800">Quick Actions</h3>
          <div className="grid grid-cols-2 gap-4">
            <button onClick={() => window.location.href = '/students'} className="p-4 bg-gray-50 rounded-xl border hover:border-blue-500 hover:bg-white transition-all text-left group">
              <span className="block text-2xl mb-2">👤</span>
              <span className="font-semibold group-hover:text-blue-600">Register Student</span>
            </button>
            <button onClick={() => window.location.href = '/attendance'} className="p-4 bg-gray-50 rounded-xl border hover:border-green-500 hover:bg-white transition-all text-left group">
              <span className="block text-2xl mb-2">📝</span>
              <span className="font-semibold group-hover:text-green-600">Mark Attendance</span>
            </button>
          </div>
        </div>

        <div className="bg-white p-8 rounded-2xl border shadow-sm space-y-4">
          <h3 className="text-xl font-bold text-gray-800">System Overview</h3>
          <div className="space-y-4">
            <div className="flex justify-between items-center p-3 bg-blue-50 text-blue-800 rounded-lg border border-blue-100">
              <span className="font-medium">System Status</span>
              <span className="px-2 py-1 bg-green-500 text-white text-xs rounded-full font-bold">Stable</span>
            </div>
            <div className="p-4 text-sm text-gray-600 bg-gray-50 rounded-lg">
              All services are running normally. Regular backups are being processed as scheduled.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
