"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { useSession } from "@/context/SessionContext";

export default function SessionSetupPage() {
    const router = useRouter();
    const { refreshSessions } = useSession();
    const [name, setName] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleSubmit = async (e: React.FormEvent) => {
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

            // Redirect to dashboard
            router.push("/");
        } catch (err: any) {
            console.error("Failed to create session", err);
            setError(err.response?.data?.message || "Failed to create session");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
            <div className="max-w-md w-full p-8 bg-white rounded-lg shadow-md">
                <h1 className="text-2xl font-bold text-center mb-6 text-gray-800">
                    Set Up Academic Session
                </h1>

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
            </div>
        </div>
    );
}
