import Link from "next/link";

export default function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      <aside className="w-64 bg-gray-900 text-white p-4">
        <h2 className="text-xl font-bold mb-6">School Admin</h2>

        <nav className="space-y-3">
          <Link href="/dashboard">Dashboard</Link>
          <Link href="/schools">Schools</Link>
          <Link href="/students">Students</Link>
          <Link href="/fees">Fees</Link>
        </nav>
      </aside>

      <main className="flex-1 bg-gray-100 p-6">{children}</main>
    </div>
  );
}

