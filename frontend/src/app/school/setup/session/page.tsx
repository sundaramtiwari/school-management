"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { useSession } from "@/context/SessionContext";
import { useAuth } from "@/context/AuthContext";

export default function SessionSetupPage() {
    const router = useRouter();
    const { user } = useAuth();
    const { refreshSessions, hasSession } = useSession();
    const [name, setName] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    // Prevent duplicate session creation
    useEffect(() => {
        if (hasSession) {
            router.push("/");
        }
    }, [hasSession, router]);

    if (hasSession) {
        return null; // Don't render form if session exists
    }

    const canCreateSession = user?.role === "SCHOOL_ADMIN";

    const handleSubmit = async (e: React.FormEvent) => {
        if (!canCreateSession) return;
        e.preventDefault();
        setLoading(true);
        setError("");

        try {
            await api.post("/api/academic-sessions", {
                name,
                active: true, // First session is active by default
            });

            // Refresh session context to pick up the new session and currentSessionId
            await refreshSessions();

            // Wait for session context to update by checking hasSession
            // Give it a moment for state to propagate
            setTimeout(() => {
                router.push("/");
            }, 100);
        } catch (err: any) {
            console.error("Failed to create session", err);
            setError(err.response?.data?.message || "Failed to create session");
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
            <div className="max-w-md w-full p-8 bg-white rounded-lg shadow-md">
                <h1 className="text-2xl font-bold text-center mb-6 text-gray-800">
                    Set Up Academic Session
                </h1>

                {canCreateSession ? (
                    <>
                        <p className="text-gray-600 mb-6 text-center">
                            Welcome! To get started, please create your first academic session (e.g., "2024-2025").
                        </p>

                        {error && (
                            <div className="bg-red-50 text-red-600 p-3 rounded mb-4 text-sm">
                                {error}
                            </div>
                        )}

                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Session Name
                                </label>
                                <input
                                    type="text"
                                    required
                                    placeholder="e.g. 2024-2025"
                                    className="w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    value={name}
                                    onChange={(e) => setName(e.target.value)}
                                />
                            </div>

                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition disabled:opacity-50"
                            >
                                {loading ? "Creating..." : "Create Session"}
                            </button>
                        </form>
                    </>
                ) : (
                    <div className="text-center py-6">
                        <div className="bg-orange-50 text-orange-700 p-4 rounded-md mb-6 border border-orange-100">
                            <p className="font-medium mb-1">Action Required</p>
                            <p className="text-sm">
                                No academic session is configured for your school.
                                Please contact your School Administrator to set up the current session.
                            </p>
                        </div>
                        <button
                            onClick={() => router.push("/login")}
                            className="text-blue-600 hover:underline text-sm font-medium"
                        >
                            Back to Login
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}
