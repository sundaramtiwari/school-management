"use client";

import { AuthProvider, useAuth } from "@/context/AuthContext";
import { SessionProvider } from "@/context/SessionContext";
import Sidebar from "@/components/Sidebar";
import Breadcrumbs from "@/components/Breadcrumbs";
import { useRouter, usePathname } from "next/navigation";
import { useSession } from "@/context/SessionContext";
import { useEffect } from "react";
import ErrorBoundary from "@/components/ErrorBoundary";

import SuspensionOverlay from "@/components/subscription/SuspensionOverlay";
import { useSubscription } from "@/context/SubscriptionProvider";

function LayoutContent({ children }: { children: React.ReactNode }) {
    const { user, isLoading: authLoading } = useAuth();
    const { currentSession, isSessionLoading: sessionLoading } = useSession();
    const { subscriptionStatus, schoolActive, isLoading: subLoading } = useSubscription();
    const pathname = usePathname();
    const router = useRouter();

    useEffect(() => {
        // Redirection logic:
        // 1. User belongs to a school (user.schoolId)
        // 2. Session loading is finished (!sessionLoading)
        // 3. No session exists (!currentSession)
        // 4. Role is authorized to CREATE sessions (SCHOOL_ADMIN)
        // 5. Not already on setup page or login page

        const canCreateSession = user?.role === "SCHOOL_ADMIN";
        const isOnSetupPage = pathname.startsWith("/school/setup");
        const isLoginPage = pathname.startsWith("/login");

        if (
            user &&
            user.schoolId &&
            !sessionLoading &&
            !currentSession &&
            canCreateSession &&
            !isOnSetupPage &&
            !isLoginPage
        ) {
            router.push("/school/setup/session");
        }
    }, [user, currentSession, sessionLoading, pathname, router]);

    if (authLoading || sessionLoading || subLoading) {
        return <div className="flex h-screen items-center justify-center">Loading...</div>;
    }

    const titleMap: Record<string, string> = {
        "/subscription": "Subscription & Billing",
        // Add other operational pages if needed
    };

    const isLoginPage = pathname === "/login";
    const isPlatformPage = pathname.startsWith("/platform");
    const isOperationalPage = !isLoginPage && !isPlatformPage && !pathname.startsWith("/subscription");

    // BLOCKING: Inactive School Enforcement
    if (!schoolActive && isOperationalPage) {
        return (
            <div className="flex min-h-screen">
                {!isLoginPage && user && <Sidebar />}
                <main className="flex-1 flex items-center justify-center p-8">
                    <div className="max-w-md w-full text-center space-y-4 bg-white p-8 rounded-2xl shadow-xl border border-red-100">
                        <div className="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto text-4xl shadow-inner">🚫</div>
                        <h2 className="text-2xl font-black text-gray-900 uppercase tracking-tight">Access Locked</h2>
                        <p className="text-gray-500 text-sm leading-relaxed">
                            Your school account is <span className="text-red-600 font-bold uppercase">Inactive</span>.
                            Operations are currently restricted. Please contact platform support to resume.
                        </p>
                        <div className="pt-4">
                            <button
                                onClick={() => router.push("/subscription")}
                                className="w-full bg-gray-900 text-white py-3 rounded-lg font-bold hover:bg-black transition-all shadow-lg"
                            >
                                VIEW BILLING STATUS
                            </button>
                        </div>
                    </div>
                </main>
            </div>
        );
    }

    return (
        <div className="flex min-h-screen">
            {!isLoginPage && user && <Sidebar />}
            <main className={`flex-1 p-8 overflow-auto ${!user ? "w-full" : ""}`}>
                {!isLoginPage && user && <Breadcrumbs />}
                {isOperationalPage && <SuspensionOverlay />}
                {children}
            </main>
        </div>
    );
}


import { SubscriptionProvider } from "@/context/SubscriptionProvider";

export default function ClientLayout({ children }: { children: React.ReactNode }) {
    return (
        <ErrorBoundary>
            <AuthProvider>
                <SessionProvider>
                    <SubscriptionProvider>
                        <LayoutContent>{children}</LayoutContent>
                    </SubscriptionProvider>
                </SessionProvider>
            </AuthProvider>
        </ErrorBoundary>
    );
}

