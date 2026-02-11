"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { Skeleton } from "@/components/ui/Skeleton";
import { useActiveSession } from "@/hooks/useActiveSession";

export default function SchoolAdminDashboard() {
  const { user } = useAuth();
  const { session: activeSession, loading: sessionLoading } = useActiveSession();
  const [stats, setStats] = useState({
    students: 0,
    classes: 0,
    teachers: 0,
    attendance: 0,
    feesPending: 0,
    feesCollected: 0,
  });
  const [loading, setLoading] = useState(true);
  const [schoolName, setSchoolName] = useState("Your School");

  useEffect(() => {
    if (!sessionLoading && activeSession) loadDashboardData();
  }, [sessionLoading, activeSession]);

  async function loadDashboardData() {
    try {
      setLoading(true);

      // Load school info
      if (user?.schoolId) {
        const schoolRes = await api.get(`/api/schools/${user.schoolId}`);
        setSchoolName(schoolRes.data.name || "Your School");
      }

      // Load stats (parallel requests — allSettled so one failure doesn't break all)
      const results = await Promise.allSettled([
        api.get('/api/students?size=1'),
        api.get('/api/classes?size=1'),
        api.get('/api/users?role=TEACHER&size=1'),
        api.get('/api/attendance/stats/today'),
        api.get(`/api/fees/summary/stats?session=${activeSession}`),
      ]);

      const val = (idx: number) => results[idx].status === 'fulfilled' ? (results[idx] as any).value.data : null;

      setStats({
        students: val(0)?.totalElements || 0,
        classes: val(1)?.totalElements || 0,
        teachers: val(2)?.totalElements || 0,
        attendance: val(3)?.percentage || 0,
        feesPending: val(4)?.pending || 0,
        feesCollected: val(4)?.collected || 0,
      });
    } catch (err) {
      console.error("Failed to load dashboard", err);
    } finally {
      setLoading(false);
    }
  }

  const cards = [
    {
      label: "Total Students",
      value: stats.students,
      color: "bg-blue-500",
      icon: "👨‍🎓",
      description: "Enrolled students"
    },
    {
      label: "Classes",
      value: stats.classes,
      color: "bg-green-500",
      icon: "📚",
      description: "Active classes"
    },
    {
      label: "Teachers",
      value: stats.teachers,
      color: "bg-purple-500",
      icon: "👨‍🏫",
      description: "Teaching staff"
    },
    {
      label: "Today's Attendance",
      value: `${stats.attendance}%`,
      color: "bg-yellow-500",
      icon: "✓",
      description: "Present today"
    },
    {
      label: "Fees Collected",
      value: `₹ ${stats.feesCollected.toLocaleString('en-IN')}`,
      color: "bg-emerald-500",
      icon: "💰",
      description: "This month"
    },
    {
      label: "Pending Dues",
      value: `₹ ${stats.feesPending.toLocaleString('en-IN')}`,
      color: "bg-red-500",
      icon: "⏰",
      description: "Outstanding"
    },
  ];

  const quickActions = [
    {
      title: "Enroll Student",
      icon: "➕",
      hoverBorder: "hover:border-blue-500",
      hoverBg: "hover:bg-blue-50",
      hoverText: "group-hover:text-blue-600",
      href: "/students"
    },
    {
      title: "Create Teacher",
      icon: "👥",
      hoverBorder: "hover:border-purple-500",
      hoverBg: "hover:bg-purple-50",
      hoverText: "group-hover:text-purple-600",
      href: "/staff"
    },
    {
      title: "Collect Fee",
      icon: "💵",
      hoverBorder: "hover:border-green-500",
      hoverBg: "hover:bg-green-50",
      hoverText: "group-hover:text-green-600",
      href: "/fees/collect"
    },
    {
      title: "Mark Attendance",
      icon: "✓",
      hoverBorder: "hover:border-yellow-500",
      hoverBg: "hover:bg-yellow-50",
      hoverText: "group-hover:text-yellow-600",
      href: "/attendance"
    },
    {
      title: "Manage Transport",
      icon: "🚌",
      hoverBorder: "hover:border-orange-500",
      hoverBg: "hover:bg-orange-50",
      hoverText: "group-hover:text-orange-600",
      href: "/transport"
    },
    {
      title: "Fee Structures",
      icon: "📊",
      hoverBorder: "hover:border-indigo-500",
      hoverBg: "hover:bg-indigo-50",
      hoverText: "group-hover:text-indigo-600",
      href: "/fees/structures"
    },
  ];

  return (
    <div className="space-y-8">
      {/* School Header */}
      <header className="bg-gradient-to-r from-blue-600 to-blue-700 text-white p-8 rounded-2xl shadow-lg">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 bg-white/20 rounded-full flex items-center justify-center text-3xl">
            🏫
          </div>
          <div>
            <h1 className="text-3xl font-bold">{schoolName}</h1>
            <p className="text-blue-100 mt-1">School Administration Dashboard</p>
            <p className="text-blue-200 text-sm mt-1">Session: {activeSession || "Loading..."}</p>
          </div>
        </div>
      </header>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {cards.map((card, i) => (
          <div key={i} className="bg-white p-6 rounded-2xl border shadow-sm hover:shadow-md transition-all">
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">
                  {card.label}
                </p>
                {loading ? (
                  <Skeleton className="h-10 w-24 mt-2" />
                ) : (
                  <p className="text-3xl font-bold text-gray-900 mt-2">
                    {card.value}
                  </p>
                )}
                <p className="text-xs text-gray-400 mt-1">{card.description}</p>
              </div>
              <div className={`w-12 h-12 ${card.color} text-white rounded-xl flex items-center justify-center text-xl shadow-lg`}>
                {card.icon}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Quick Actions */}
        <div className="bg-white p-8 rounded-2xl border shadow-sm">
          <h3 className="text-xl font-bold text-gray-800 mb-6">Quick Actions</h3>
          <div className="grid grid-cols-2 gap-4">
            {quickActions.map((action, i) => (
              <button
                key={i}
                onClick={() => window.location.href = action.href}
                className={`
                  p-4 rounded-xl border-2 ${action.hoverBorder} 
                  ${action.hoverBg} transition-all text-left group
                  bg-gray-50 hover:shadow-md
                `}
              >
                <span className="block text-2xl mb-2">{action.icon}</span>
                <span className={`font-semibold text-gray-700 ${action.hoverText}`}>
                  {action.title}
                </span>
              </button>
            ))}
          </div>
        </div>

        {/* Recent Activity / Alerts */}
        <div className="bg-white p-8 rounded-2xl border shadow-sm">
          <h3 className="text-xl font-bold text-gray-800 mb-6">Alerts & Notifications</h3>
          <div className="space-y-4">
            {stats.feesPending > 0 && (
              <div className="flex gap-3 p-4 bg-red-50 text-red-800 rounded-lg border border-red-100">
                <span className="text-xl">⚠️</span>
                <div>
                  <p className="font-semibold">Fee Defaulters</p>
                  <p className="text-sm text-red-600">
                    ₹ {stats.feesPending.toLocaleString('en-IN')} pending from students
                  </p>
                </div>
              </div>
            )}

            {stats.attendance < 80 && (
              <div className="flex gap-3 p-4 bg-yellow-50 text-yellow-800 rounded-lg border border-yellow-100">
                <span className="text-xl">📉</span>
                <div>
                  <p className="font-semibold">Low Attendance</p>
                  <p className="text-sm text-yellow-600">
                    Today's attendance is {stats.attendance}% - below 80%
                  </p>
                </div>
              </div>
            )}

            <div className="flex gap-3 p-4 bg-green-50 text-green-800 rounded-lg border border-green-100">
              <span className="text-xl">✓</span>
              <div>
                <p className="font-semibold">System Status</p>
                <p className="text-sm text-green-600">
                  All systems operational
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
