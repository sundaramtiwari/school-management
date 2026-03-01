"use client";

import React, { useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { AuthProvider, useAuth } from "@/context/AuthContext";

const PLATFORM_ROLES = ["SUPER_ADMIN", "PLATFORM_ADMIN"];

const platformNav = [
    { name: "Dashboard", path: "/platform/dashboard", icon: "📊" },
    { name: "Schools", path: "/platform/schools", icon: "🏫" },
    { name: "Pricing Plans", path: "/platform/pricing-plans", icon: "🏷️" },
    { name: "Subscriptions", path: "/platform/subscriptions", icon: "💳" },
];

function PlatformSidebar() {
    const pathname = usePathname();
    const router = useRouter();
    const { user, logout } = useAuth();

    return (
        <aside className="w-56 bg-white border-r flex flex-col h-screen shrink-0">
            <div className="p-4 border-b">
                <div className="text-sm font-extrabold text-indigo-600 tracking-widest uppercase">
                    Platform Admin
                </div>
                <div className="text-xs text-gray-400 mt-0.5">{user?.role?.replace("_", " ")}</div>
            </div>

            <nav className="p-2 space-y-1 flex-1 overflow-y-auto">
                {platformNav.map((item) => {
                    const active =
                        item.path === "/platform/subscriptions"
                            ? pathname.startsWith("/platform/subscriptions")
                            : pathname === item.path ||
                            (item.path !== "/" && pathname.startsWith(item.path));
                    return (
                        <Link
                            key={item.path}
                            href={item.path}
                            className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors
                ${active ? "bg-indigo-50 text-indigo-700 font-semibold" : "text-gray-600 hover:bg-gray-100"}`}
                        >
                            <span>{item.icon}</span>
                            <span>{item.name}</span>
                        </Link>
                    );
                })}
            </nav>

            <div className="border-t p-4">
                <button
                    onClick={logout}
                    className="w-full flex items-center gap-2 justify-center bg-red-50 text-red-600 py-2 rounded-lg hover:bg-red-100 text-sm font-semibold border border-red-100"
                >
                    <span>🚪</span> Logout
                </button>
            </div>
        </aside>
    );
}

function PlatformGuard({ children }: { children: React.ReactNode }) {
    const { user, isLoading } = useAuth();
    const router = useRouter();

    useEffect(() => {
        if (!isLoading && !user) {
            router.replace("/login");
        }
        if (!isLoading && user && !PLATFORM_ROLES.includes(user.role?.toUpperCase())) {
            // Non-platform role — redirect to tenant dashboard
            router.replace("/");
        }
    }, [user, isLoading, router]);

    if (isLoading) {
        return (
            <div className="flex h-screen items-center justify-center text-gray-400 text-sm">
                Loading...
            </div>
        );
    }

    if (!user || !PLATFORM_ROLES.includes(user.role?.toUpperCase())) {
        return null;
    }

    return (
        <div className="flex min-h-screen">
            <PlatformSidebar />
            <main className="flex-1 overflow-auto p-8 bg-gray-50">{children}</main>
        </div>
    );
}

export default function PlatformLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return (
        <AuthProvider>
            <PlatformGuard>{children}</PlatformGuard>
        </AuthProvider>
    );
}
