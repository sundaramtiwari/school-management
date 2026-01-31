import Link from "next/link";

export default function Sidebar() {
  return (
    <aside className="w-64 bg-slate-900 text-white min-h-screen p-4">
      <h2 className="text-xl font-bold mb-6">School Admin</h2>

      <nav className="space-y-3">
        <Link className="block hover:text-blue-400" href="/">
          Dashboard
        </Link>

        <Link className="block hover:text-blue-400" href="/schools">
          Schools
        </Link>

        <Link className="block hover:text-blue-400" href="/students">
          Students
        </Link>

        <Link className="block hover:text-blue-400" href="/fees">
          Fees
        </Link>
      </nav>
    </aside>
  );
}

