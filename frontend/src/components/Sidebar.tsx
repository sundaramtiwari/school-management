"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { useSession } from "@/context/SessionContext";
import { useToast } from "@/components/ui/Toast";

const menu = [
  {
    name: "Dashboard",
    path: "/",
    icon: "📊",
    roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT"]
  },
  {
    name: "Schools",
    path: "/schools",
    icon: "🏫",
    roles: ["SUPER_ADMIN", "PLATFORM_ADMIN"]
  },
  {
    name: "Academics",
    icon: "🎓",
    roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT"],
    children: [
      { name: "Students", path: "/students", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT"] },
      { name: "Classes", path: "/classes", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER"] },
      { name: "Subjects", path: "/subjects", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER"] },
      { name: "Staff", path: "/staff", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN"] },
      { name: "Teacher Assignments", path: "/staff/assignments", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN"] },
      { name: "Attendance", path: "/attendance", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER"] },
      { name: "Exams", path: "/exams", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER"] },
      { name: "Marksheet", path: "/marksheets", roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN", "TEACHER"] },
    ]
  },
  {
    name: "Finance",
    icon: "💵",
    roles: ["SUPER_ADMIN", "SCHOOL_ADMIN", "ACCOUNTANT"],
    children: [
      { name: "Fee Collection", path: "/fees/collect", roles: ["SUPER_ADMIN", "SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Fee Summary", path: "/fees/summary", roles: ["SUPER_ADMIN", "SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Defaulters", path: "/fees/defaulters", roles: ["SUPER_ADMIN", "SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Fee Structures", path: "/fees/structures", roles: ["SUPER_ADMIN", "SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Daily Cash", path: "/finance/daily-cash", roles: ["SUPER_ADMIN", "SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Monthly P&L", path: "/finance/monthly-pl", roles: ["SUPER_ADMIN", "SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Session P&L", path: "/finance/session-pl", roles: ["SUPER_ADMIN", "SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Expenses", path: "/finance/expenses", roles: ["SUPER_ADMIN", "SCHOOL_ADMIN", "ACCOUNTANT"] },
    ]
  },
  {
    name: "Transport",
    path: "/transport",
    icon: "🚌",
    roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN"]
  },
  {
    name: "Sessions",
    path: "/sessions",
    icon: "📅",
    roles: ["SUPER_ADMIN", "PLATFORM_ADMIN", "SCHOOL_ADMIN"]
  },
];

const REQUIRES_SESSION = [
  "Academics", "Finance", "Classes", "Students", "Fees", "Marksheets", "Attendance", "Exams",
  "Fee Collection", "Fee Summary", "Fee Structures", "Daily Cash", "Expenses", "Marksheet"
];

const REQUIRES_CLASSES = [
  "Students", "Fees", "Marksheets", "Attendance", "Exams", "Finance",
  "Fee Collection", "Fee Summary", "Fee Structures", "Daily Cash", "Expenses", "Marksheet"
];

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
  const [schoolId, setSchoolId] = useState<string | null>(null);
  const [schoolName, setSchoolName] = useState<string | null>(null);
  const [expandedMenus, setExpandedMenus] = useState<Record<string, boolean>>({});

  useEffect(() => {
    setSchoolId(localStorage.getItem("schoolId"));
    setSchoolName(localStorage.getItem("schoolName"));

    const newExpanded: Record<string, boolean> = {};
    menu.forEach(item => {
      if (item.children?.some(child => pathname === child.path)) {
        newExpanded[item.name] = true;
      }
    });
    setExpandedMenus(newExpanded);
  }, [pathname]);

  const toggleMenu = (menuName: string) => {
    setExpandedMenus(prev => ({ ...prev, [menuName]: !prev[menuName] }));
  };

  const checkRestriction = (name: string) => {
    const userRole = user?.role?.toUpperCase();

    // GATING LOGIC
    // 1. !currentSession (No Session) -> Disable everything except Dashboard, Staff, Transport, Sessions
    // 2. currentSession && !hasClasses -> Enable Classes. Disable Students, Fees, Attendance, Marksheets, Finance.

    const isSchoolScoped = ["SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT"].includes(userRole as string);
    const isPlatformRole = ["SUPER_ADMIN", "PLATFORM_ADMIN"].includes(userRole as string);

    if (isSchoolScoped) {
      if (!currentSession && REQUIRES_SESSION.includes(name)) {
        return {
          isRestricted: true,
          message: "Please create an academic session first.",
          redirectPath: "/school/setup/session"
        };
      }
      if (currentSession && !hasClasses && REQUIRES_CLASSES.includes(name)) {
        return {
          isRestricted: true,
          message: "Please create at least one class to access this section.",
          redirectPath: "/classes"
        };
      }
    }

    // Platform Role Gating: If no school selected, block school-specific links
    // School-specific modules exclude: Dashboard, Schools, Staff, Sessions, Transport
    const isSchoolModule = !["Dashboard", "Schools", "Staff", "Sessions", "Transport"].includes(name);

    // Group headers themselves should also be gated if they enclose school modules
    const isGroupHeader = ["Academics", "Finance"].includes(name);

    if (isPlatformRole && (isSchoolModule || isGroupHeader) && !schoolId) {
      return {
        isRestricted: true,
        message: "Please select a school first from the Schools list.",
        redirectPath: "/schools"
      };
    }

    return { isRestricted: false, message: "", redirectPath: "" };
  };

  const clearContext = () => {
    localStorage.removeItem("schoolId");
    localStorage.removeItem("schoolName");
    window.location.reload();
  };

  return (
    <aside className="w-64 bg-white border-r flex flex-col h-screen">
      <div className="p-4 border-b">
        <div className="text-xl font-bold text-blue-600 tracking-tight">
          {getRoleDisplay(user?.role)}
        </div>
      </div>

      <nav className="p-2 space-y-1 flex-1 overflow-y-auto">
        {menu.map((item) => {
          const userRole = user?.role?.toUpperCase();
          const hasAccess = item.roles.includes(userRole as string);
          if (!hasAccess) return null;

          const active = pathname === item.path || (item.children && item.children.some(child => pathname === child.path));
          const isExpanded = expandedMenus[item.name];
          const restriction = checkRestriction(item.name);

          if (item.children) {
            return (
              <div key={item.name} className="block">
                <button
                  onClick={() => {
                    if (restriction.isRestricted) {
                      showToast(restriction.message, "warning");
                      if (restriction.redirectPath) router.push(restriction.redirectPath);
                    } else {
                      toggleMenu(item.name);
                    }
                  }}
                  className={`
                    w-full px-4 py-2.5 rounded-lg flex items-center justify-between transition-all
                    ${active ? "bg-blue-50 text-blue-700 font-bold" : "text-gray-700 hover:bg-gray-100"}
                    ${restriction.isRestricted ? "opacity-50 cursor-not-allowed" : ""}
                  `}
                >
                  <div className="flex items-center gap-3">
                    <span className="text-lg">{item.icon}</span>
                    <span className="text-sm font-semibold tracking-wide uppercase">{item.name}</span>
                  </div>
                  <span className={`transform transition-transform text-[10px] ${isExpanded ? "rotate-180" : ""}`}>
                    ▼
                  </span>
                </button>

                {isExpanded && !restriction.isRestricted && (
                  <div className="ml-9 mt-1 space-y-0.5 border-l-2 border-blue-50 pl-2">
                    {item.children.map(child => {
                      const childHasAccess = child.roles.includes(userRole as string);
                      if (!childHasAccess) return null;

                      const childActive = pathname === child.path;
                      const childRestriction = checkRestriction(child.name);

                      return (
                        <Link
                          key={child.path}
                          href={childRestriction.isRestricted || !child.path ? "#" : child.path}
                          onClick={(e) => {
                            if (childRestriction.isRestricted) {
                              e.preventDefault();
                              showToast(childRestriction.message, "warning");
                              if (childRestriction.redirectPath) router.push(childRestriction.redirectPath);
                            }
                          }}
                          className={`
                            block px-3 py-2 text-sm rounded-lg transition-colors
                            ${childActive ? "bg-blue-100 text-blue-800 font-semibold" : "text-gray-600 hover:bg-gray-100 hover:text-gray-900"}
                            ${childRestriction.isRestricted ? "opacity-50 cursor-not-allowed" : ""}
                          `}
                        >
                          {child.name}
                        </Link>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          }

          return (
            <Link
              key={item.path || item.name}
              href={restriction.isRestricted || !item.path ? "#" : item.path}
              onClick={(e) => {
                if (restriction.isRestricted) {
                  e.preventDefault();
                  showToast(restriction.message, "warning");
                  if (restriction.redirectPath) router.push(restriction.redirectPath);
                }
              }}
              className={`
                block px-4 py-2.5 rounded-lg flex items-center gap-3 transition-all
                ${active ? "bg-blue-100 text-blue-700 font-bold" : "text-gray-700 hover:bg-gray-100"}
                ${restriction.isRestricted ? "opacity-50 cursor-not-allowed" : ""}
              `}
            >
              <span className="text-lg">{item.icon}</span>
              <span className="text-sm">{item.name}</span>
            </Link>
          );
        })}
      </nav>

      {schoolName && (user?.role === "SUPER_ADMIN" || user?.role === "PLATFORM_ADMIN") && (
        <div className="mx-2 mb-2 p-3 bg-blue-50 rounded-lg flex items-center justify-between text-[11px] border border-blue-100 shadow-sm">
          <div className="flex items-center gap-2 text-blue-700 font-medium truncate">
            <span>📍</span> {schoolName}
          </div>
          <button onClick={clearContext} className="text-red-500 hover:text-red-700 font-black p-1">
            ✕
          </button>
        </div>
      )}

      <div className="border-t p-4">
        <button
          onClick={logout}
          className="w-full flex items-center justify-center gap-2 bg-red-50 text-red-600 px-4 py-2.5 rounded-lg hover:bg-red-100 transition-all font-semibold text-sm border border-red-100"
        >
          <span>🚪</span> Logout
        </button>
      </div>
    </aside>
  );
}
