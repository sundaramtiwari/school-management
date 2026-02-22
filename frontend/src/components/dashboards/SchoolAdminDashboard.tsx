"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { Skeleton } from "@/components/ui/Skeleton";
import { useSession } from "@/context/SessionContext";

export default function SchoolAdminDashboard() {
  type UpcomingExam = {
    name: string;
    className: string;
    date: string;
  };

  const router = useRouter();
  const { user } = useAuth();
  const { currentSession, isSessionLoading: sessionLoading } = useSession();
  const [stats, setStats] = useState({
    students: 0,
    classes: 0,
    teachers: 0,
    attendance: 0,
    feesPending: 0,
    feesCollected: 0,
    transportCount: 0,
    feePendingCount: 0,
    upcomingExams: [] as UpcomingExam[],
  });
  const [loading, setLoading] = useState(true);
  const [schoolName, setSchoolName] = useState("Your School");

  const loadDashboardData = useCallback(async () => {
    if (!currentSession?.id) return;
    try {
      setLoading(true);

      // Load school info
      if (user?.schoolId) {
        const schoolRes = await api.get(`/api/schools/id/${user.schoolId}`);
        setSchoolName(schoolRes.data.name || "Your School");
      }

      // Load stats via new dedicated endpoint
      const [results, classCountRes] = await Promise.all([
        api.get(`/api/dashboard/school-admin/stats?sessionId=${currentSession.id}`),
        api.get('/api/classes/count'), // Classes still separate as they are global-ish
      ]);

      const data = results.data;
      const classData = classCountRes.data;

      setStats({
        students: data.totalStudents || 0,
        classes: classData?.count || 0,
        teachers: data.totalTeachers || 0,
        attendance: data.attendancePercentage || 0,
        feesPending: data.totalFeesPending || 0, // Should be amount or adapted
        feesCollected: data.totalFeesCollected || 0,
        transportCount: data.transportCount || 0,
        feePendingCount: data.feePendingCount || 0,
        upcomingExams: data.upcomingExams || [],
      });
    } catch (err) {
      console.error("Failed to load dashboard", err);
    } finally {
      setLoading(false);
    }
  }, [currentSession?.id, user?.schoolId]);

  useEffect(() => {
    if (!sessionLoading && currentSession) loadDashboardData();
  }, [sessionLoading, currentSession, loadDashboardData]);

  const cards = [
    {
      label: "Total Students",
      value: stats.students,
      color: "bg-blue-500",
      icon: "👨‍🎓",
      description: "Enrolled students"
    },
    {
      label: "Transport Users",
      value: stats.transportCount,
      color: "bg-orange-500",
      icon: "🚌",
      description: "Active enrollments"
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
      label: "Fee Defaulters",
      value: stats.feePendingCount,
      color: "bg-red-500",
      icon: "📉",
      description: "Students with dues"
    },
    {
      label: "Classes",
      value: stats.classes,
      color: "bg-green-500",
      icon: "📚",
      description: "Active sections"
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

  if (!sessionLoading && !currentSession) {
    return (
      <div className="p-10 bg-white border-2 border-dashed border-blue-200 rounded-3xl text-center">
        <span className="text-5xl mb-4 block">🎓</span>
        <h2 className="text-2xl font-black text-gray-900">Welcome! Let&apos;s set up your school</h2>
        <p className="text-gray-500 mt-2 mb-8 max-w-sm mx-auto">Create your first academic session to start managing students, attendance, and fees.</p>
        <button
          onClick={() => router.push("/school/setup/session")}
          className="bg-blue-600 text-white px-10 py-3 rounded-2xl font-black shadow-xl hover:bg-blue-700 transition-all"
        >
          Initialize First Session →
        </button>
      </div>
    );
  }

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
            <p className="text-blue-200 text-sm mt-1">
              {sessionLoading
                ? "Loading Session..."
                : currentSession
                  ? `Session: ${currentSession.name}`
                  : "No Academic Session Created"
              }
            </p>
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
                onClick={() => router.push(action.href)}
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

        {/* Recent Activity / Alerts / Exams */}
        <div className="bg-white p-8 rounded-2xl border shadow-sm">
          <h3 className="text-xl font-bold text-gray-800 mb-6">Upcoming Exams & Alerts</h3>
          <div className="space-y-4">
            {stats.upcomingExams.length > 0 && (
              <div className="mb-6">
                <p className="text-sm font-semibold text-gray-500 uppercase mb-3">Next 5 Exams</p>
                <div className="space-y-2">
                  {stats.upcomingExams.map((exam, idx) => (
                    <div key={idx} className="flex items-center justify-between p-3 bg-indigo-50 rounded-lg border border-indigo-100">
                      <div>
                        <p className="font-semibold text-indigo-900">{exam.name}</p>
                        <p className="text-xs text-indigo-700">{exam.className}</p>
                      </div>
                      <div className="text-right">
                        <p className="font-mono text-sm font-bold text-indigo-800">{exam.date}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {stats.feePendingCount > 0 && (
              <div className="flex gap-3 p-4 bg-red-50 text-red-800 rounded-lg border border-red-100">
                <span className="text-xl">⚠️</span>
                <div>
                  <p className="font-semibold">Fee Defaulters</p>
                  <p className="text-sm text-red-600">
                    {stats.feePendingCount} students have outstanding dues in {currentSession?.name}
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
                    Today&apos;s attendance is {stats.attendance}% - below 80%
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
