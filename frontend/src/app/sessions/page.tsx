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
    const { sessions, currentSession, refreshSessions, isSessionLoading: isLoading } = useSession();
    const canManage = user?.role === "SCHOOL_ADMIN" || user?.role === "SUPER_ADMIN" || user?.role === "PLATFORM_ADMIN";
    const [isSaving, setIsSaving] = useState(false);
    const [showForm, setShowForm] = useState(false);
    const [showInlineCreate, setShowInlineCreate] = useState(false);
    const [editId, setEditId] = useState<number | null>(null);

    const [createForm, setCreateForm] = useState({
        name: "",
        startDate: "",
        endDate: "",
        active: true,
    });

    const [form, setForm] = useState({
        name: "",
        startDate: "",
        endDate: "",
        active: true,
    });

    async function saveSessionEdit() {
        if (!editId || !form.name) {
            showToast("Please fill all required fields", "warning");
            return;
        }

        try {
            setIsSaving(true);
            await sessionApi.update(editId, { name: form.name, active: form.active });
            showToast("Academic session updated!", "success");
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

    async function saveNewSessionInline() {
        if (!createForm.name || !createForm.startDate || !createForm.endDate) {
            showToast("Please fill all required fields", "warning");
            return;
        }
        if (new Date(createForm.endDate) < new Date(createForm.startDate)) {
            showToast("End date must be on or after start date", "warning");
            return;
        }

        try {
            setIsSaving(true);
            await sessionApi.create(createForm);
            showToast("Academic session created!", "success");
            setShowInlineCreate(false);
            setCreateForm({ name: "", startDate: "", endDate: "", active: true });
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
        } catch (e: unknown) {
            const msg = (e as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
                || (e as { message?: string })?.message
                || "Unknown error";
            showToast("Failed to set current session: " + msg, "error");
        }
    }

    function openCreate() {
        setCreateForm({ name: "", startDate: "", endDate: "", active: true });
        setShowInlineCreate(true);
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
        <div className="mx-auto px-6 py-6 space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-lg font-semibold">Academic Sessions</h1>
                    <p className="text-gray-500 text-base mt-1">Manage school years and current active session.</p>
                </div>
                {canManage && (
                    <button
                        onClick={openCreate}
                        className="bg-blue-600 text-white px-6 py-2.5 rounded-md font-medium hover:bg-blue-700 flex items-center gap-2 text-base"
                    >
                        <span className="text-xl">+</span> New Session
                    </button>
                )}
            </div>

            {canManage && showInlineCreate && (
                <div className="bg-white rounded-lg shadow border border-gray-100 p-6">
                    <div className="flex items-center justify-between mb-4">
                        <h2 className="text-lg font-bold text-gray-800">Create New Session</h2>
                        <button
                            onClick={() => setShowInlineCreate(false)}
                            className="text-gray-500 hover:text-gray-700 text-sm font-semibold"
                        >
                            Cancel
                        </button>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
                        <div>
                            <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Session Name *</label>
                            <input
                                value={createForm.name}
                                onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
                                placeholder="e.g. 2026-2027"
                                className="w-full rounded-md border border-gray-300 px-3 py-2 text-base focus:ring-2 focus:ring-blue-500"
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Start Date *</label>
                            <input
                                type="date"
                                value={createForm.startDate}
                                onChange={(e) => setCreateForm({ ...createForm, startDate: e.target.value })}
                                className="w-full rounded-md border border-gray-300 px-3 py-2 text-base focus:ring-2 focus:ring-blue-500"
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">End Date *</label>
                            <input
                                type="date"
                                value={createForm.endDate}
                                onChange={(e) => setCreateForm({ ...createForm, endDate: e.target.value })}
                                className="w-full rounded-md border border-gray-300 px-3 py-2 text-base focus:ring-2 focus:ring-blue-500"
                            />
                        </div>
                        <div>
                            <button
                                onClick={saveNewSessionInline}
                                disabled={isSaving}
                                className="w-full px-6 py-3 rounded-md bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50"
                            >
                                {isSaving ? "Creating..." : "Create Session"}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {isLoading ? (
                <div className="bg-white rounded-lg shadow border border-gray-100 p-6">
                    <TableSkeleton rows={5} cols={4} />
                </div>
            ) : (
                <div className="bg-white rounded-lg shadow border border-gray-100 overflow-hidden mt-4">
                    <table className="w-full text-base">
                        <thead className="bg-gray-50 text-gray-600 text-lg font-semibold border-b border-gray-100">
                            <tr>
                                <th className="px-6 py-4 text-left">Academic Year</th>
                                <th className="px-6 py-4 text-center">Period</th>
                                <th className="px-6 py-4 text-center">Status Flag</th>
                                <th className="px-6 py-4 text-center w-48">Actions</th>
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
                title="Edit Session"
                maxWidth="max-w-md"
                footer={
                    <div className="flex gap-2">
                        <button
                            onClick={() => setShowForm(false)}
                            className="px-6 py-2 rounded-md bg-white border border-gray-300 font-medium text-gray-600 hover:bg-gray-50"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={saveSessionEdit}
                            disabled={isSaving}
                            className="px-8 py-2 rounded-md bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50"
                        >
                            {isSaving ? "Saving..." : "Save Changes"}
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
                            className="w-full rounded-md border border-gray-300 px-3 py-2 text-base focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                    <div className="p-4 rounded-lg border border-gray-100 bg-gray-50 text-gray-500 text-base">
                        Session dates are fixed after creation and cannot be edited.
                    </div>

                    <label className="flex items-center gap-3 cursor-pointer p-4 rounded-lg border border-gray-100 bg-gray-50">
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
