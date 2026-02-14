"use client";

import { AuthProvider, useAuth } from "@/context/AuthContext";
import { SessionProvider } from "@/context/SessionContext";
import Sidebar from "@/components/Sidebar";
import Breadcrumbs from "@/components/Breadcrumbs";
import { useRouter, usePathname } from "next/navigation";
import { useSession } from "@/context/SessionContext";
import { useEffect } from "react";

function LayoutContent({ children }: { children: React.ReactNode }) {
    const { user, isLoading: authLoading } = useAuth();
    const { currentSession, isLoading: sessionLoading } = useSession();
    const pathname = usePathname();
    const router = useRouter();

    useEffect(() => {
        // STRICT: Only SCHOOL_ADMIN is forced to have a session
        if (
            user &&
            user.role === "SCHOOL_ADMIN" &&
            !sessionLoading &&
            !currentSession &&
            !pathname.startsWith("/school/setup") &&
            !pathname.startsWith("/login")
        ) {
            router.push("/school/setup/session");
        }
    }, [user, currentSession, sessionLoading, pathname, router]);

    if (authLoading || sessionLoading) {
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
