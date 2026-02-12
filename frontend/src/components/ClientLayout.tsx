"use client";

import { AuthProvider, useAuth } from "@/context/AuthContext";
import { SessionProvider } from "@/context/SessionContext";
import Sidebar from "@/components/Sidebar";
import Breadcrumbs from "@/components/Breadcrumbs";
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
            <main className={`flex-1 p-8 overflow-auto ${!user ? "w-full" : ""}`}>
                {!isLoginPage && user && <Breadcrumbs />}
                {children}
            </main>
        </div>
    );
}

export default function ClientLayout({ children }: { children: React.ReactNode }) {
    return (
        <AuthProvider>
            <SessionProvider>
                <LayoutContent>{children}</LayoutContent>
            </SessionProvider>
        </AuthProvider>
    );
}
