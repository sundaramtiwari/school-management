"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

const menu = [
  { name: "Dashboard", path: "/" },
  { name: "Schools", path: "/schools" },
  { name: "Students", path: "/students" },
  { name: "Classes", path: "/classes" },
  { name: "Staff", path: "/staff" },
  { name: "Fees", path: "/fees" },
  { name: "Attendance", path: "/attendance" },
  { name: "Marksheets", path: "/marksheets" },
  { name: "Sessions", path: "/sessions" },
];

export default function Sidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  return (
    <aside className="w-64 bg-white border-r flex flex-col h-screen">

      {/* Header */}
      <div className="p-4 text-xl font-bold border-b">
        School Admin
      </div>

      {/* Menu */}
      <nav className="p-2 space-y-1 flex-1 overflow-y-auto">

        {menu.map((item) => {
          const active = pathname === item.path;

          return (
            <Link
              key={item.path}
              href={item.path}
              className={`
                block px-4 py-2 rounded
                ${active
                  ? "bg-blue-100 text-blue-700 font-semibold"
                  : "text-gray-700 hover:bg-gray-100"
                }
              `}
            >
              {item.name}
            </Link>
          );
        })}

      </nav>

      {/* User Info & Logout */}
      <div className="border-t p-4 space-y-2">
        <div className="text-sm text-gray-600">
          <div className="font-semibold">{user?.role || "User"}</div>
          <div className="text-xs text-gray-500">
            {user?.schoolId ? `School ID: ${user.schoolId}` : "Platform Admin"}
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
