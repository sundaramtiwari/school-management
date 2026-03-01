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
    roles: ["SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT"]
  },
  {
    name: "Academics",
    icon: "🎓",
    roles: ["SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT"],
    children: [
      { name: "Students", path: "/students", icon: "👥", roles: ["SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT"] },
      { name: "Classes", path: "/classes", icon: "🏢", roles: ["SCHOOL_ADMIN", "TEACHER"] },
      { name: "Subjects", path: "/subjects", icon: "📚", roles: ["SCHOOL_ADMIN", "TEACHER"] },
      { name: "Staff", path: "/staff", icon: "👨‍🏫", roles: ["SCHOOL_ADMIN"] },
      { name: "Teacher Assignments", path: "/staff/assignments", icon: "📝", roles: ["SCHOOL_ADMIN"] },
      { name: "Attendance", path: "/attendance", icon: "📅", roles: ["SCHOOL_ADMIN", "TEACHER"] },
      { name: "Exams", path: "/exams", icon: "✍️", roles: ["SCHOOL_ADMIN", "TEACHER"] },
      { name: "Marksheet", path: "/marksheets", icon: "📜", roles: ["SCHOOL_ADMIN", "TEACHER"] },
    ]
  },
  {
    name: "Finance",
    icon: "💵",
    roles: ["SCHOOL_ADMIN", "ACCOUNTANT"],
    children: [
      { name: "Fee Summary", path: "/fees/summary", icon: "📋", roles: ["SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Fee Structures", path: "/fees/structures", icon: "⚙️", roles: ["SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Fee Discounts", path: "/fees/discounts", icon: "🏷️", roles: ["SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Fee Collection", path: "/fees/collect", icon: "💰", roles: ["SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Fee Defaulters", path: "/fees/defaulters", icon: "⚠️", roles: ["SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Finance Overview", path: "/finance", icon: "📊", roles: ["SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Expenses", path: "/finance/expenses", icon: "💸", roles: ["SCHOOL_ADMIN", "ACCOUNTANT"] },
      { name: "Expense Heads", path: "/finance/expense-heads", icon: "🏷️", roles: ["SCHOOL_ADMIN", "ACCOUNTANT"] },
    ]
  },
  {
    name: "Transport",
    path: "/transport",
    icon: "🚌",
    roles: ["SCHOOL_ADMIN"]
  },
  {
    name: "Sessions",
    path: "/sessions",
    icon: "📅",
    roles: ["SCHOOL_ADMIN"]
  },
  // SCHOOL_ADMIN only — flat link
  {
    name: "My Subscription",
    path: "/subscription",
    icon: "💳",
    roles: ["SCHOOL_ADMIN"]
  },
];

const REQUIRES_SESSION = [
  "Academics", "Finance", "Classes", "Students", "Fees", "Marksheets", "Attendance", "Exams",
  "Fee Collection", "Fee Summary", "Fee Structures", "Finance Overview", "Expenses", "Marksheet"
];

const REQUIRES_CLASSES = [
  "Students", "Fees", "Marksheets", "Attendance", "Exams", "Finance",
  "Fee Collection", "Fee Summary", "Fee Structures", "Finance Overview", "Expenses", "Marksheet"
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
  const [isCollapsed, setIsCollapsed] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem("sidebar-collapsed");
    if (saved === "true") setIsCollapsed(true);
  }, []);

  const toggleSidebar = () => {
    setIsCollapsed(prev => {
      const newVal = !prev;
      localStorage.setItem("sidebar-collapsed", String(newVal));
      return newVal;
    });
  };

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
    // Tenant Sidebar is ONLY shown to SCHOOL_ADMIN, TEACHER, ACCOUNTANT.
    // Platform roles use PlatformLayout's sidebar.
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
    return { isRestricted: false, message: "", redirectPath: "" };
  };

  const clearContext = () => {
    localStorage.removeItem("schoolId");
    localStorage.removeItem("schoolName");
    window.location.reload();
  };

  return (
    <aside className={`${isCollapsed ? "w-20" : "w-64"} bg-white border-r flex flex-col h-screen transition-all duration-300 relative group`}>
      {/* Collapse Toggle Button */}
      <button
        onClick={toggleSidebar}
        className="absolute -right-3 top-10 bg-white border border-gray-200 rounded-full w-6 h-6 flex items-center justify-center shadow-sm hover:shadow-md hover:bg-gray-50 z-50 transition-all text-[10px] text-gray-400 hover:text-blue-600"
        title={isCollapsed ? "Expand Sidebar" : "Collapse Sidebar"}
      >
        {isCollapsed ? "→" : "←"}
      </button>

      <div className={`p-4 border-b flex items-center ${isCollapsed ? "justify-center" : "justify-between"}`}>
        <div className={`text-xl font-extrabold text-blue-600 tracking-tighter transition-all duration-300 ${isCollapsed ? "scale-110" : ""}`}>
          {isCollapsed ? "S" : getRoleDisplay(user?.role)}
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
                      if (isCollapsed) setIsCollapsed(false);
                      toggleMenu(item.name);
                    }
                  }}
                  className={`
                    w-full px-4 py-2.5 rounded-lg flex items-center justify-between transition-all group/item
                    ${active ? "bg-blue-50 text-blue-700 font-bold" : "text-gray-700 hover:bg-gray-100"}
                    ${restriction.isRestricted ? "opacity-50 cursor-not-allowed" : ""}
                    ${isCollapsed ? "justify-center" : ""}
                  `}
                  title={isCollapsed ? item.name : ""}
                >
                  <div className="flex items-center gap-3">
                    <span className={`text-lg transition-transform ${isCollapsed ? "group-hover/item:scale-110" : ""}`}>{item.icon}</span>
                    {!isCollapsed && <span className="text-sm font-semibold tracking-wide uppercase whitespace-nowrap overflow-hidden">{item.name}</span>}
                  </div>
                  {!isCollapsed && (
                    <span className={`transform transition-transform text-[10px] ${isExpanded ? "rotate-180" : ""}`}>
                      ▼
                    </span>
                  )}
                </button>

                {isExpanded && !restriction.isRestricted && !isCollapsed && (
                  <div className="ml-9 mt-1 space-y-0.5 border-l-2 border-blue-50 pl-2 overflow-hidden animate-in slide-in-from-top-2 duration-200">
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
                          <div className="flex items-center gap-2">
                            {/* @ts-ignore */}
                            {child.icon && <span className="text-base">{child.icon}</span>}
                            <span>{child.name}</span>
                          </div>
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
                px-4 py-2.5 rounded-lg flex items-center transition-all group/item
                ${active ? "bg-blue-100 text-blue-700 font-bold" : "text-gray-700 hover:bg-gray-100"}
                ${restriction.isRestricted ? "opacity-50 cursor-not-allowed" : ""}
                ${isCollapsed ? "justify-center" : "gap-3"}
              `}
              title={isCollapsed ? item.name : ""}
            >
              <span className={`text-lg transition-transform ${isCollapsed ? "group-hover/item:scale-110" : ""}`}>{item.icon}</span>
              {!isCollapsed && <span className="text-sm whitespace-nowrap overflow-hidden">{item.name}</span>}
            </Link>
          );
        })}
      </nav>

      {schoolName && !isCollapsed && (user?.role === "SUPER_ADMIN" || user?.role === "PLATFORM_ADMIN") && (
        <div className="mx-2 mb-2 p-3 bg-blue-50 rounded-lg flex items-center justify-between text-[11px] border border-blue-100 shadow-sm animate-in fade-in duration-300">
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
          className={`
            w-full flex items-center justify-center bg-red-50 text-red-600 py-2.5 rounded-lg hover:bg-red-100 transition-all font-semibold text-sm border border-red-100
            ${isCollapsed ? "px-0" : "px-4 gap-2"}
          `}
          title={isCollapsed ? "Logout" : ""}
        >
          <span>🚪</span> {!isCollapsed && "Logout"}
        </button>
      </div>
    </aside>
  );
}
