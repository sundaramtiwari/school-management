"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { useSession } from "@/context/SessionContext";
import { useToast } from "@/components/ui/Toast";

const menu = [
  { name: "Dashboard", path: "/", icon: "📊", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT"] },
  { name: "Schools", path: "/schools", icon: "🏫", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN"] },
  { name: "Students", path: "/students", icon: "👨‍🎓", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT"] },
  { name: "Classes", path: "/classes", icon: "📚", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER"] },
  { name: "Subjects", path: "/subjects", icon: "📖", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER"] },
  { name: "Staff", path: "/staff", icon: "👥", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN"] },
  { name: "Fees", path: "/fees", icon: "💰", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "ACCOUNTANT"] },
  { name: "Transport", path: "/transport", icon: "🚌", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN"] },
  { name: "Attendance", path: "/attendance", icon: "✓", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER"] },
  { name: "Exams", path: "/exams", icon: "📝", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER"] },
  { name: "Marksheets", path: "/marksheets", icon: "📊", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER"] },
  { name: "Sessions", path: "/sessions", icon: "📅", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN"] },
];

// ✅ FIXED: Proper role display mapping
const getRoleDisplay = (role: string | undefined): string => {
  const displays: Record<string, string> = {
    SUPER_ADMIN: "Platform Owner",
    PLATFORM_ADMIN: "Platform Admin",
    SCHOOL_ADMIN: "School Admin",
    TEACHER: "Teacher Portal",
    ACCOUNTANT: "Finance Portal",
    PARENT: "Parent Portal"
  };
  return displays[role?.toUpperCase() || ""] || "User Portal";
};

export default function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout } = useAuth();
  const { currentSession, hasClasses } = useSession();
  const { showToast } = useToast();

  const [schoolName, setSchoolName] = useState<string | null>(null);

  useEffect(() => {
    setSchoolName(localStorage.getItem("schoolName"));
  }, []);

  const clearContext = () => {
    localStorage.removeItem("schoolId");
    localStorage.removeItem("schoolName");
    window.location.reload();
  };

  return (
    <aside className="w-64 bg-white border-r flex flex-col h-screen">

      <div className="p-4 border-b">
        <div className="text-xl font-bold text-blue-600">
          {getRoleDisplay(user?.role)}
        </div>
        {currentSession && (
          <div className="mt-1 px-2 py-0.5 bg-blue-50 text-blue-700 text-[10px] font-black uppercase rounded border border-blue-100 inline-block">
            Session {currentSession.name}
          </div>
        )}
      </div>

      {/* Menu - ADDED ICONS */}
      <nav className="p-2 space-y-1 flex-1 overflow-y-auto">

        {menu.map((item) => {
          const active = pathname === item.path;
          const userRole = user?.role?.toUpperCase();
          const hasAccess = item.roles.includes(userRole as string);

          if (!hasAccess) return null;

          // GATING LOGIC
          // 1. !currentSession (No Session) -> Disable everything except Dashboard, Staff, Transport, Sessions
          //    (Sessions is effectively /school/setup/session redirect or manual management)
          // 2. currentSession && !hasClasses -> Enable Classes. Disable Students, Fees, Attendance, Marksheets.

          const isSchoolScoped = ["SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT"].includes(userRole as string);
          const isPlatformRole = ["SUPER_ADMIN", "PLATFORM_ADMIN"].includes(userRole as string);

          const requiresSession = ["Classes", "Students", "Fees", "Marksheets", "Attendance"].includes(item.name);
          const requiresClasses = ["Students", "Fees", "Marksheets", "Attendance"].includes(item.name);

          let isRestricted = false;
          let restrictionMessage = "";
          let redirectPath = "";

          if (isSchoolScoped) {
            if (!currentSession && requiresSession) {
              // State 1: No Session
              isRestricted = true;
              restrictionMessage = "Please create an academic session first.";
              redirectPath = "/school/setup/session";
            } else if (currentSession && !hasClasses && requiresClasses) {
              // State 2: Session exists, no classes -> Classes allowed, others restricted
              isRestricted = true;
              restrictionMessage = "Please create at least one class to access this section.";
              redirectPath = "/classes";
            }
          }

          // Platform Role Gating: If no school selected, block school-specific links
          const isSchoolModule = !["Dashboard", "Schools", "Staff"].includes(item.name);
          if (isPlatformRole && isSchoolModule && !localStorage.getItem("schoolId")) {
            isRestricted = true;
            restrictionMessage = "Please select a school first from the Schools list.";
            redirectPath = "/schools";
          }

          return (
            <Link
              key={item.path}
              href={isRestricted ? "#" : item.path}
              onClick={(e) => {
                if (isRestricted) {
                  e.preventDefault();
                  showToast(restrictionMessage, "warning");
                  if (redirectPath) {
                    router.push(redirectPath);
                  }
                }
              }}
              className={`
                block px-4 py-2.5 rounded-lg flex items-center gap-3
                ${active
                  ? "bg-blue-100 text-blue-700 font-semibold"
                  : "text-gray-700 hover:bg-gray-100"
                }
                ${isRestricted ? "opacity-50 cursor-not-allowed" : ""}
              `}
            >
              <span className="text-lg">{item.icon}</span>
              <span>{item.name}</span>
            </Link>
          );
        })}

      </nav>

      {/* School Context Banner for Platform Roles */}
      {schoolName && (user?.role === "SUPER_ADMIN" || user?.role === "PLATFORM_ADMIN") && (
        <div className="border-t p-4 bg-gray-50 flex items-center justify-between text-[11px] font-bold">
          <div className="flex items-center gap-2 text-gray-600 truncate">
            <span>📍</span> Viewing: {schoolName}
          </div>
          <button
            onClick={clearContext}
            className="text-red-500 hover:text-red-700 ml-2 font-black flex items-center gap-1"
          >
            ✕ Exit
          </button>
        </div>
      )}

      {/* User Info & Logout */}
      <div className="border-t p-4 space-y-2">
        <div className="text-sm text-gray-600">
          <div className="font-semibold">{getRoleDisplay(user?.role)}</div>
          <div className="text-xs text-gray-500">
            {user?.schoolId ? `School ID: ${user.schoolId}` : "Platform Level"}
          </div>
        </div>
        <button
          onClick={logout}
          className="w-full bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 transition"
        >
          Logout
        </button>
      </div>
    </aside>
  );
}
