"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { Skeleton } from "@/components/ui/Skeleton";
import { useSession } from "@/context/SessionContext";

type ClassInfo = {
  id: number;
  name: string;
  section: string;
  session: string;
  studentCount: number;
  attendanceMarked: boolean;
};

type PendingTask = {
  type: "attendance" | "marks" | "exam";
  title: string;
  description: string;
  classId: number;
  className: string;
};

export default function TeacherDashboard() {
  type ClassApiItem = {
    id: number;
    name: string;
    section: string;
    session: string;
  };

  const { currentSession } = useSession();
  const router = useRouter();

  const [classes, setClasses] = useState<ClassInfo[]>([]);
  const [pendingTasks, setPendingTasks] = useState<PendingTask[]>([]);
  const [stats, setStats] = useState({
    totalClasses: 0,
    totalStudents: 0,
    attendanceMarkedToday: 0,
    pendingMarkEntry: 0,
  });
  const [loading, setLoading] = useState(true);
  const [todayDate] = useState(new Date().toISOString().split('T')[0]);

  const loadTeacherDashboard = useCallback(async () => {
    if (!currentSession) return;
    try {
      setLoading(true);

      // Load teacher's assigned classes using explicit sessionId
      const classesRes = await api.get<ClassApiItem[]>(`/api/class-subjects/my-classes?sessionId=${currentSession.id}`);
      const classData: ClassApiItem[] = classesRes.data || [];

      // Enrich each class with attendance status
      const enrichedClasses = await Promise.all(
        classData.map(async (cls: ClassApiItem) => {
          try {
            // Check if attendance marked today
            const attendanceRes = await api.get(
              `/api/attendance/class/${cls.id}?sessionId=${currentSession.id}&date=${todayDate}`
            );
            const hasAttendance = (attendanceRes.data || []).length > 0;

            // Get student count
            const studentRes = await api.get(`/api/students/by-class/${cls.id}?size=1`);
            const studentCount = studentRes.data?.totalElements || 0;

            return {
              id: cls.id,
              name: cls.name,
              section: cls.section,
              session: cls.session,
              studentCount,
              attendanceMarked: hasAttendance,
            };
          } catch {
            return {
              id: cls.id,
              name: cls.name,
              section: cls.section,
              session: cls.session,
              studentCount: 0,
              attendanceMarked: false,
            };
          }
        })
      );

      setClasses(enrichedClasses);

      // Calculate stats
      const totalStudents = enrichedClasses.reduce((sum, cls) => sum + cls.studentCount, 0);
      const attendanceMarked = enrichedClasses.filter(cls => cls.attendanceMarked).length;

      let publishedExamsCount = 0;
      await Promise.all(
        classData.map(async (cls: ClassApiItem) => {
          try {
            const examRes = await api.get(`/api/exams/by-class/${cls.id}`);
            const exams = examRes.data || [];
            const publishedExams = Array.isArray(exams)
              ? exams.filter((ex: { status: string }) => ex.status === "PUBLISHED")
              : (exams.content || []).filter((ex: { status: string }) => ex.status === "PUBLISHED");
            publishedExamsCount += publishedExams.length;
          } catch {
            // ignore
          }
        })
      );

      setStats({
        totalClasses: enrichedClasses.length,
        totalStudents,
        attendanceMarkedToday: attendanceMarked,
        pendingMarkEntry: publishedExamsCount,
      });

      // Build pending tasks
      const tasks: PendingTask[] = [];
      enrichedClasses.forEach(cls => {
        if (!cls.attendanceMarked) {
          tasks.push({
            type: "attendance",
            title: "Mark Attendance",
            description: `${cls.name}-${cls.section} attendance not marked today`,
            classId: cls.id,
            className: `${cls.name}-${cls.section}`,
          });
        }
      });

      setPendingTasks(tasks);

    } catch (err) {
      console.error("Failed to load teacher dashboard", err);
    } finally {
      setLoading(false);
    }
  }, [todayDate, currentSession]);

  useEffect(() => {
    loadTeacherDashboard();
  }, [loadTeacherDashboard]);

  const quickActions = [
    {
      title: "Mark Attendance",
      icon: "✓",
      color: "green",
      href: "/attendance",
      description: "Record today's presence",
      disabled: false
    },
    {
      title: "Enter Marks",
      icon: "📝",
      color: "blue",
      href: "/marksheets",
      description: "Update exam scores",
      disabled: false
    },
    {
      title: "View Students",
      icon: "👨‍🎓",
      color: "purple",
      href: "/students",
      description: "Student directory",
      disabled: false
    },
    {
      title: "View Exams",
      icon: "📋",
      color: "indigo",
      href: "/exams",
      description: "Manage assessments",
      disabled: false
    },
  ];

  return (
    <div className="space-y-8">
      {/* Welcome Header */}
      <header className="bg-gradient-to-r from-purple-600 to-purple-700 text-white p-8 rounded-2xl shadow-lg">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 bg-white/20 rounded-full flex items-center justify-center text-3xl">
            👨‍🏫
          </div>
          <div>
            <h1 className="text-3xl font-bold">Welcome, Teacher!</h1>
            <p className="text-purple-100 mt-1">Your Teaching Dashboard</p>
            <p className="text-purple-200 text-sm mt-1">
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

      {/* Stats Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white p-6 rounded-2xl border shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase">My Classes</p>
              {loading ? (
                <Skeleton className="h-10 w-16 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">{stats.totalClasses}</p>
              )}
              <p className="text-xs text-gray-400 mt-1">Assigned to you</p>
            </div>
            <div className="w-12 h-12 bg-purple-500 text-white rounded-xl flex items-center justify-center text-xl">
              📚
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase">Total Students</p>
              {loading ? (
                <Skeleton className="h-10 w-16 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">{stats.totalStudents}</p>
              )}
              <p className="text-xs text-gray-400 mt-1">Across all classes</p>
            </div>
            <div className="w-12 h-12 bg-blue-500 text-white rounded-xl flex items-center justify-center text-xl">
              👨‍🎓
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase">Attendance Today</p>
              {loading ? (
                <Skeleton className="h-10 w-20 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">
                  {stats.attendanceMarkedToday}/{stats.totalClasses}
                </p>
              )}
              <p className="text-xs text-gray-400 mt-1">Classes marked</p>
            </div>
            <div className="w-12 h-12 bg-green-500 text-white rounded-xl flex items-center justify-center text-xl">
              ✓
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase">Pending Tasks</p>
              {loading ? (
                <Skeleton className="h-10 w-16 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">{pendingTasks.length}</p>
              )}
              <p className="text-xs text-gray-400 mt-1">Action items</p>
            </div>
            <div className="w-12 h-12 bg-orange-500 text-white rounded-xl flex items-center justify-center text-xl">
              ⏰
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* My Classes */}
        <div className="bg-white p-8 rounded-2xl border shadow-sm">
          <h3 className="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
            <span>📚</span> My Classes
          </h3>
          <div className="space-y-3">
            {loading ? (
              <>
                <Skeleton className="h-16 w-full" />
                <Skeleton className="h-16 w-full" />
                <Skeleton className="h-16 w-full" />
              </>
            ) : classes.length === 0 ? (
              <div className="text-center py-12 text-gray-400">
                <p className="text-4xl mb-3">📚</p>
                <p className="text-sm">No classes assigned yet</p>
              </div>
            ) : (
              classes.map((cls) => (
                <div
                  key={cls.id}
                  className="flex items-center justify-between p-4 border rounded-xl hover:bg-gray-50 transition-all"
                >
                  <div className="flex-1">
                    <p className="font-bold text-gray-800">
                      {cls.name}-{cls.section}
                    </p>
                    <p className="text-sm text-gray-500">
                      {cls.studentCount} students • Session: {cls.session}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    {cls.attendanceMarked ? (
                      <span className="px-3 py-1 bg-green-100 text-green-700 text-xs rounded-full font-bold">
                        ✓ Marked
                      </span>
                    ) : (
                      <span className="px-3 py-1 bg-red-100 text-red-700 text-xs rounded-full font-bold">
                        Pending
                      </span>
                    )}
                    <button
                      onClick={() => router.push(`/attendance?class=${cls.id}`)}
                      className="text-blue-600 hover:text-blue-800 text-sm font-semibold"
                    >
                      Mark →
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Pending Tasks & Quick Actions */}
        <div className="space-y-8">
          {/* Pending Tasks */}
          <div className="bg-white p-8 rounded-2xl border shadow-sm">
            <h3 className="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
              <span>⏰</span> Pending Tasks
            </h3>
            <div className="space-y-3">
              {loading ? (
                <>
                  <Skeleton className="h-16 w-full" />
                  <Skeleton className="h-16 w-full" />
                </>
              ) : pendingTasks.length === 0 ? (
                <div className="text-center py-8 text-gray-400">
                  <p className="text-4xl mb-3">✓</p>
                  <p className="text-sm font-semibold text-green-600">All caught up!</p>
                  <p className="text-xs mt-1">No pending tasks</p>
                </div>
              ) : (
                pendingTasks.slice(0, 5).map((task, i) => (
                  <div
                    key={i}
                    className="flex gap-3 p-4 bg-yellow-50 border border-yellow-100 rounded-lg"
                  >
                    <span className="text-xl">
                      {task.type === "attendance" ? "✓" : "📝"}
                    </span>
                    <div className="flex-1">
                      <p className="font-semibold text-gray-800">{task.title}</p>
                      <p className="text-sm text-gray-600">{task.description}</p>
                    </div>
                    <button
                      onClick={() => {
                        if (task.type === "attendance") {
                          router.push(`/attendance?class=${task.classId}`);
                        }
                      }}
                      className="text-blue-600 hover:text-blue-800 text-sm font-bold"
                    >
                      Do →
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Quick Actions */}
          <div className="bg-white p-8 rounded-2xl border shadow-sm">
            <h3 className="text-xl font-bold text-gray-800 mb-6">Quick Actions</h3>
            <div className="grid grid-cols-2 gap-4">
              {quickActions.map((action, i) => (
                <button
                  key={i}
                  onClick={() => !action.disabled && router.push(action.href)}
                  disabled={action.disabled}
                  title={action.disabled ? "Feature coming soon" : ""}
                  className={`
                    p-4 rounded-xl border-2 border-gray-100
                    ${action.disabled ? "opacity-50 cursor-not-allowed" : `hover:border-${action.color}-400 hover:bg-${action.color}-50`}
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
        </div>
      </div>
    </div>
  );
}
