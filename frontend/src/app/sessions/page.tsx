"use client";

import { useEffect, useState } from "react";
import { sessionApi } from "@/lib/sessionApi";
import { useAuth } from "@/context/AuthContext";

type Session = {
    id: number;
    name: string;
    startDate: string;
    endDate: string;
    isCurrent: boolean;
    active: boolean;
};

export default function SessionsPage() {
    const { user } = useAuth();
    const [sessions, setSessions] = useState<Session[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [showForm, setShowForm] = useState(false);

    const [form, setForm] = useState({
        name: "",
        startDate: "",
        endDate: "",
        isCurrent: false,
    });

    useEffect(() => {
        if (user?.schoolId) {
            loadSessions();
        }
    }, [user?.schoolId]);

    async function loadSessions() {
        try {
            setLoading(true);
            const res = await sessionApi.list(user!.schoolId!);
            setSessions(res.data);
        } catch {
            setError("Failed to load sessions");
        } finally {
            setLoading(false);
        }
    }

    async function saveSession() {
        if (!form.name || !form.startDate || !form.endDate) {
            alert("Please fill all required fields");
            return;
        }

        try {
            const payload = { ...form, schoolId: user?.schoolId };
            await sessionApi.create(payload);
            setShowForm(false);
            setForm({ name: "", startDate: "", endDate: "", isCurrent: false });
            loadSessions();
        } catch (e: any) {
            alert("Save failed: " + (e.response?.data?.message || e.message));
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold">Academic Sessions</h1>
                <button
                    onClick={() => setShowForm(true)}
                    className="bg-blue-600 text-white px-4 py-2 rounded"
                >
                    + Add Session
                </button>
            </div>

            {loading && <p>Loading...</p>}
            {error && <p className="text-red-500">{error}</p>}

            {!loading && (
                <div className="bg-white border rounded">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-gray-100">
                            <tr>
                                <th className="p-3">Name</th>
                                <th className="p-3">Start Date</th>
                                <th className="p-3">End Date</th>
                                <th className="p-3">Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            {sessions.map((s) => (
                                <tr key={s.id} className="border-t">
                                    <td className="p-3 font-medium">
                                        {s.name} {s.isCurrent && <span className="ml-2 text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded">Current</span>}
                                    </td>
                                    <td className="p-3">{s.startDate}</td>
                                    <td className="p-3">{s.endDate}</td>
                                    <td className="p-3">
                                        {s.active ? (
                                            <span className="text-green-600">Active</span>
                                        ) : (
                                            <span className="text-gray-400">Inactive</span>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {showForm && (
                <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
                    <div className="bg-white p-6 rounded w-[400px] space-y-4 shadow-xl">
                        <h2 className="font-semibold text-lg border-b pb-2">Add New Session</h2>

                        <div className="space-y-3">
                            <div>
                                <label className="block text-xs font-medium text-gray-500 mb-1">Session Name</label>
                                <input
                                    value={form.name}
                                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                                    placeholder="e.g. 2024-25"
                                    className="w-full border p-2 rounded"
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="block text-xs font-medium text-gray-500 mb-1">Start Date</label>
                                    <input
                                        type="date"
                                        value={form.startDate}
                                        onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                                        className="w-full border p-2 rounded"
                                    />
                                </div>
                                <div>
                                    <label className="block text-xs font-medium text-gray-500 mb-1">End Date</label>
                                    <input
                                        type="date"
                                        value={form.endDate}
                                        onChange={(e) => setForm({ ...form, endDate: e.target.value })}
                                        className="w-full border p-2 rounded"
                                    />
                                </div>
                            </div>

                            <label className="flex items-center gap-2 cursor-pointer pt-2">
                                <input
                                    type="checkbox"
                                    checked={form.isCurrent}
                                    onChange={(e) => setForm({ ...form, isCurrent: e.target.checked })}
                                />
                                <span className="text-sm">Set as Current Session</span>
                            </label>
                        </div>

                        <div className="flex justify-end gap-2 pt-4 border-t">
                            <button
                                onClick={() => setShowForm(false)}
                                className="px-4 py-2 text-gray-500"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={saveSession}
                                className="bg-blue-600 text-white px-6 py-2 rounded font-medium"
                            >
                                Save Session
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
