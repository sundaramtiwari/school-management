import "./globals.css";
import Sidebar from "@/components/Sidebar";

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="bg-gray-100">

        <div className="flex min-h-screen">

          {/* Sidebar */}
          <Sidebar />

          {/* Main Area */}
          <main className="flex-1 p-6 overflow-auto">
            {children}
          </main>

        </div>

      </body>
    </html>
  );
}
