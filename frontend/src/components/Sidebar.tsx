"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { useSession } from "@/context/SessionContext";

const menu = [
  { name: "Dashboard", path: "/", icon: "📊", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT"] },
  { name: "Schools", path: "/schools", icon: "🏫", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN"] },
  { name: "Students", path: "/students", icon: "👨‍🎓", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT"] },
  { name: "Classes", path: "/classes", icon: "📚", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER"] },
  { name: "Staff", path: "/staff", icon: "👥", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN"] },
  { name: "Fees", path: "/fees", icon: "💰", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "ACCOUNTANT"] },
  { name: "Transport", path: "/transport", icon: "🚌", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN"] },
  { name: "Attendance", path: "/attendance", icon: "✓", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER"] },
  { name: "Marksheets", path: "/marksheets", icon: "📝", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER"] },
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
  const { user, logout } = useAuth();
  const { currentSession, hasClasses } = useSession();

  return (
    <aside className="w-64 bg-white border-r flex flex-col h-screen">

      {/* Header - FIXED */}
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

          // Class-based gating
          const restrictedWithoutClasses = ["Students", "Fees", "Marksheets", "Attendance"].includes(item.name);
          const isRestricted = restrictedWithoutClasses && !hasClasses && userRole === "SCHOOL_ADMIN";

          return (
            <Link
              key={item.path}
              href={isRestricted ? "/classes" : item.path}
              onClick={(e) => {
                if (isRestricted) {
                  e.preventDefault();
                  alert("Please create at least one class to access this section.");
                  // You might want to use a toast here if available, but alert is safe for now as requested "banner/toast"
                  // router.push("/classes"); // Link href handles it if we don't prevent default, but we want to intercept.
                  // Actually, if we prevent default, we must push manually.
                  // But cleaner is to let the href be /classes and just show the alert?
                  // No, users prefer staying on the same page or explicit redirect.
                  // Let's use simpler approach:
                  // If restricted, clicking it shows alert and goes to /classes.
                  window.location.href = "/classes";
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

      {/* User Info & Logout */}
      <div className="border-t p-4 space-y-2">
        <div className="text-sm text-gray-600">
          {/* FIXED: Better role display */}
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
