"use client";

import { AuthProvider, useAuth } from "@/context/AuthContext";
import Sidebar from "@/components/Sidebar";
import { usePathname } from "next/navigation";

function LayoutContent({ children }: { children: React.ReactNode }) {
    const { user, isLoading } = useAuth();
    const pathname = usePathname();

    if (isLoading) {
        return <div className="flex h-screen items-center justify-center">Loading...</div>;
    }

    const isLoginPage = pathname === "/login";

    return (
        <div className="flex min-h-screen">
            {!isLoginPage && user && <Sidebar />}
            <main className={`flex-1 p-6 overflow-auto ${!user ? "w-full" : ""}`}>
                {children}
            </main>
        </div>
    );
}

export default function ClientLayout({ children }: { children: React.ReactNode }) {
    return (
        <AuthProvider>
            <LayoutContent>{children}</LayoutContent>
        </AuthProvider>
    );
}
