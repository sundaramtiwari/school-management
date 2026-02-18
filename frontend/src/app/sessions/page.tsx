"use client";

import { useState } from "react";
import { sessionApi } from "@/lib/sessionApi";
import { useAuth } from "@/context/AuthContext";
import { useSession } from "@/context/SessionContext";
import { useToast } from "@/components/ui/Toast";
import Modal from "@/components/ui/Modal";
import { TableSkeleton } from "@/components/ui/Skeleton";

type AcademicSessionItem = {
    id: number;
    name: string;
    startDate?: string;
    endDate?: string;
    active: boolean;
};

export default function SessionsPage() {
    const { user } = useAuth();
    const { showToast } = useToast();
    const { sessions, currentSession, setCurrentSession, refreshSessions, isSessionLoading: isLoading } = useSession();
    const canManage = user?.role === "SCHOOL_ADMIN" || user?.role === "SUPER_ADMIN" || user?.role === "PLATFORM_ADMIN";
    const [isSaving, setIsSaving] = useState(false);
    const [showForm, setShowForm] = useState(false);
    const [editId, setEditId] = useState<number | null>(null);

    const [form, setForm] = useState({
        name: "",
        startDate: "",
        endDate: "",
        active: true,
    });

    async function saveSession() {
        if (!form.name || (!editId && (!form.startDate || !form.endDate))) {
            showToast("Please fill all required fields", "warning");
            return;
        }
        if (!editId && new Date(form.endDate) < new Date(form.startDate)) {
            showToast("End date must be on or after start date", "warning");
            return;
        }

        try {
            setIsSaving(true);
            if (editId) {
                await sessionApi.update(editId, { name: form.name, active: form.active });
                showToast("Academic session updated!", "success");
            } else {
                await sessionApi.create(form);
                showToast("Academic session created!", "success");
            }
            setShowForm(false);
            setEditId(null);
            setForm({ name: "", startDate: "", endDate: "", active: true });
            await refreshSessions();
        } catch (e: unknown) {
            const msg = (e as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
                || (e as { message?: string })?.message
                || "Unknown error";
            showToast("Save failed: " + msg, "error");
        } finally {
            setIsSaving(false);
        }
    }

    async function handleSetCurrent(sessionId: number) {
        try {
            await sessionApi.setCurrent(sessionId);
            showToast("Current session updated!", "success");
            await refreshSessions();

            // Update context immediately
            const target = sessions.find(s => s.id === sessionId);
            if (target) {
                setCurrentSession(target);
            }
        } catch (e: unknown) {
            const msg = (e as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
                || (e as { message?: string })?.message
                || "Unknown error";
            showToast("Failed to set current session: " + msg, "error");
        }
    }

    function openCreate() {
        setEditId(null);
        setForm({ name: "", startDate: "", endDate: "", active: true });
        setShowForm(true);
    }

    function openEdit(session: AcademicSessionItem) {
        setEditId(session.id);
        setForm({
            name: session.name || "",
            startDate: session.startDate || "",
            endDate: session.endDate || "",
            active: !!session.active,
        });
        setShowForm(true);
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Academic Sessions</h1>
                    <p className="text-gray-500">Manage school years and current active session.</p>
                </div>
                {canManage && (
                    <button
                        onClick={openCreate}
                        className="bg-blue-600 text-white px-6 py-2.5 rounded-xl font-bold shadow-lg hover:bg-blue-700 transition-all flex items-center gap-2"
                    >
                        <span className="text-xl">+</span> Add Session
                    </button>
                )}
            </div>

            {isLoading ? (
                <div className="bg-white p-8 rounded-2xl border">
                    <TableSkeleton rows={5} cols={4} />
                </div>
            ) : (
                <div className="bg-white border rounded-2xl shadow-sm overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-gray-50 text-gray-600 font-bold border-b">
                            <tr>
                                <th className="p-4 text-left">Academic Year</th>
                                <th className="p-4 text-center">Period</th>
                                <th className="p-4 text-center">Status Flag</th>
                                <th className="p-4 text-center w-48">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {sessions.map((s) => (
                                <tr key={s.id} className="hover:bg-gray-50/50 transition-colors">
                                    <td className="p-4">
                                        <div className="flex items-center gap-3">
                                            <span className="font-bold text-gray-800">{s.name}</span>
                                            {currentSession?.id === s.id && (
                                                <span className="px-2 py-0.5 bg-green-100 text-green-700 text-[10px] font-black uppercase rounded-full border border-green-200">
                                                    Current
                                                </span>
                                            )}
                                        </div>
                                    </td>
                                    <td className="p-4 text-center text-xs text-gray-600 font-semibold">
                                        {(s.startDate && s.endDate) ? `${s.startDate} to ${s.endDate}` : "Dates not set"}
                                    </td>
                                    <td className="p-4 text-center">
                                        <span className={`px-3 py-1 rounded-full text-[10px] font-bold uppercase border ${s.active ? "bg-blue-50 text-blue-600 border-blue-100" : "bg-gray-50 text-gray-400 border-gray-200"
                                            }`}>
                                            {s.active ? "Active" : "Archived"}
                                        </span>
                                    </td>
                                    <td className="p-4 text-center">
                                        <div className="flex items-center justify-center gap-3">
                                            {currentSession?.id !== s.id && (
                                                <button
                                                    onClick={() => handleSetCurrent(s.id)}
                                                    className="text-green-600 hover:text-green-700 font-bold text-xs uppercase tracking-tighter"
                                                >
                                                    Set Current
                                                </button>
                                            )}
                                            {canManage && (
                                                <button
                                                    onClick={() => openEdit(s)}
                                                    className="text-blue-600 hover:text-blue-700 font-bold text-xs uppercase tracking-tighter"
                                                >
                                                    Edit
                                                </button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            {sessions.length === 0 && (
                                <tr>
                                    <td colSpan={4} className="p-20 text-center text-gray-400 italic bg-gray-50/30">
                                        No academic sessions recorded.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            )}

            <Modal
                isOpen={showForm}
                onClose={() => setShowForm(false)}
                title={editId ? "Edit Session" : "Initialize New Session"}
                maxWidth="max-w-md"
                footer={
                    <div className="flex gap-2">
                        <button
                            onClick={() => setShowForm(false)}
                            className="px-6 py-2 rounded-xl border font-medium text-gray-600 hover:bg-gray-50 transition-all"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={saveSession}
                            disabled={isSaving}
                            className="px-8 py-2 rounded-xl bg-blue-600 text-white font-bold shadow-lg hover:bg-blue-700 disabled:bg-gray-400 transition-all"
                        >
                            {isSaving ? (editId ? "Saving..." : "Creating...") : (editId ? "Save Changes" : "Start Session")}
                        </button>
                    </div>
                }
            >
                <div className="space-y-5">
                    <div>
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Session Name *</label>
                        <input
                            value={form.name}
                            onChange={(e) => setForm({ ...form, name: e.target.value })}
                            placeholder="e.g. 2025-2026"
                            className="w-full px-4 py-3 rounded-xl border bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all font-bold"
                        />
                    </div>
                    {!editId && (
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Start Date *</label>
                                <input
                                    type="date"
                                    value={form.startDate}
                                    onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                                    className="w-full px-4 py-3 rounded-xl border bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all font-bold"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">End Date *</label>
                                <input
                                    type="date"
                                    value={form.endDate}
                                    onChange={(e) => setForm({ ...form, endDate: e.target.value })}
                                    className="w-full px-4 py-3 rounded-xl border bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all font-bold"
                                />
                            </div>
                        </div>
                    )}
                    {editId && (
                        <div className="p-3 bg-gray-50 rounded-xl border text-xs text-gray-600 font-semibold">
                            Session dates are fixed after creation and cannot be edited.
                        </div>
                    )}

                    <label className="flex items-center gap-3 cursor-pointer p-4 bg-blue-50 rounded-2xl border border-dashed border-blue-200">
                        <input
                            type="checkbox"
                            checked={form.active}
                            onChange={(e) => setForm({ ...form, active: e.target.checked })}
                            className="w-5 h-5 text-blue-600 rounded border-gray-300 focus:ring-blue-500"
                        />
                        <div>
                            <span className="block text-sm font-bold text-blue-900 leading-none">Session Status</span>
                            <span className="text-[10px] text-blue-600 font-medium">Available for data entry & selection</span>
                        </div>
                    </label>
                </div>
            </Modal>
        </div>
    );
}
