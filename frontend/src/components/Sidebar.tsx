"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const menu = [
  { name: "Dashboard", path: "/" },
  { name: "Schools", path: "/schools" },
  { name: "Students", path: "/students" },
  { name: "Classes", path: "/classes" },
  { name: "Staff", path: "/staff" },
  { name: "Fees", path: "/fees" },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-64 bg-white border-r">

      {/* Header */}
      <div className="p-4 text-xl font-bold border-b">
        School Admin
      </div>

      {/* Menu */}
      <nav className="p-2 space-y-1">

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
    </aside>
  );
}
