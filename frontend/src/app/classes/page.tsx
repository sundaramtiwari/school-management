"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import SessionSelect from "@/components/SessionSelect";
import { useToast } from "@/components/ui/Toast";
import { useAuth } from "@/context/AuthContext";
import Modal from "@/components/ui/Modal";
import { TableSkeleton } from "@/components/ui/Skeleton";

type SchoolClass = {
    id: number;
    name: string;
    section: string;
    stream?: string;
    session: string;
    active: boolean;
};

export default function ClassesPage() {
    const { user } = useAuth();
    const { showToast } = useToast();
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [loading, setLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [showForm, setShowForm] = useState(false);
    const [editId, setEditId] = useState<number | null>(null);

    const [form, setForm] = useState({
        className: "",
        section: "A",
        stream: "",
        session: "2024-25",
        active: true,
    });

    useEffect(() => {
        loadClasses();
    }, []);

    async function loadClasses() {
        if (!user) return;
        try {
            setLoading(true);
            const currentRole = user?.role?.toUpperCase();
            const isHighLevelAdmin = currentRole === "SUPER_ADMIN" || currentRole === "PLATFORM_ADMIN";
            const endpoint = isHighLevelAdmin ? "/api/classes" : "/api/classes/mine";

            const res = await api.get(`${endpoint}?size=100`);
            setClasses(res.data.content || []);
        } catch {
            showToast("Failed to load classes", "error");
        } finally {
            setLoading(false);
        }
    }

    function updateField(e: any) {
        const value = e.target.type === "checkbox" ? e.target.checked : e.target.value;
        setForm({ ...form, [e.target.name]: value });
    }

    function resetForm() {
        setForm({
            className: "",
            section: "A",
            stream: "",
            session: "2024-25",
            active: true,
        });
    }

    function openEdit(c: SchoolClass) {
        setForm({
            className: c.name,
            section: c.section,
            stream: c.stream || "",
            session: c.session,
            active: c.active,
        });
        setEditId(c.id);
        setShowForm(true);
    }

    async function saveClass() {
        if (!form.className || !form.section || !form.session) {
            showToast("Required fields missing", "warning");
            return;
        }

        try {
            setIsSaving(true);
            const payload = {
                name: form.className,
                section: form.section,
                stream: form.stream || null,
                session: form.session,
                active: form.active,
            };

            if (editId) {
                await api.put(`/api/classes/${editId}`, payload);
                showToast("Class updated successfully!", "success");
            } else {
                await api.post("/api/classes", payload);
                showToast("Class created successfully!", "success");
            }
            setShowForm(false);
            setEditId(null);
            resetForm();
            loadClasses();
        } catch (e: any) {
            showToast("Save failed: " + (e.response?.data?.message || e.message), "error");
        } finally {
            setIsSaving(false);
        }
    }

    async function deleteClass(id: number) {
        // Confirmation modal would be better, but keeping it simple for now or using native if needed.
        // Actually I should use a custom confirmation flow if I have time, but let's stick to the plan.
        if (!confirm("Are you sure? This might affect students linked to this class.")) return;
        try {
            await api.delete(`/api/classes/${id}`);
            showToast("Class deleted successfully", "success");
            loadClasses();
        } catch (e: any) {
            showToast("Delete failed", "error");
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Class Management</h1>
                    <p className="text-gray-500">Define and organize academic classes and sections.</p>
                </div>
                <button
                    onClick={() => {
                        resetForm();
                        setEditId(null);
                        setShowForm(true);
                    }}
                    className="bg-blue-600 text-white px-6 py-2.5 rounded-xl font-bold shadow-lg hover:bg-blue-700 transition-all flex items-center gap-2"
                >
                    <span className="text-xl">+</span> Add Class
                </button>
            </div>

            {loading ? (
                <div className="bg-white p-8 rounded-2xl border">
                    <TableSkeleton rows={8} cols={5} />
                </div>
            ) : (
                <div className="bg-white border rounded-2xl shadow-sm overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-gray-50 text-gray-600 font-bold border-b">
                            <tr>
                                <th className="p-4 text-left">Class Name</th>
                                <th className="p-4 text-center">Section</th>
                                <th className="p-4 text-center">Stream</th>
                                <th className="p-4 text-center">Session</th>
                                <th className="p-4 text-center w-32">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {classes.map((c) => (
                                <tr key={c.id} className="hover:bg-gray-50/50 transition-colors">
                                    <td className="p-4 font-bold text-gray-800">{c.name}</td>
                                    <td className="p-4 text-center">
                                        <span className="px-2 py-1 bg-gray-100 rounded text-xs font-bold">{c.section}</span>
                                    </td>
                                    <td className="p-4 text-center text-gray-500 uppercase text-xs">{c.stream || "-"}</td>
                                    <td className="p-4 text-center font-medium text-blue-600">{c.session}</td>
                                    <td className="p-4 text-center">
                                        <div className="flex justify-center gap-2">
                                            <button
                                                onClick={() => openEdit(c)}
                                                className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-all"
                                            >
                                                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-5M16.5 3.5a2.121 2.121 0 113 3L11.707 15.364a2 2 0 01-.88.524l-4 1a1 1 0 01-1.213-1.213l1-4a2 2 0 01.524-.88L16.5 3.5z" />
                                                </svg>
                                            </button>
                                            <button
                                                onClick={() => deleteClass(c.id)}
                                                className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-all"
                                            >
                                                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                </svg>
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            {classes.length === 0 && (
                                <tr>
                                    <td colSpan={5} className="p-20 text-center text-gray-400 italic bg-gray-50/30">
                                        No classes defined yet.
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
                title={editId ? "Edit Class Details" : "Add New Academic Class"}
                maxWidth="max-w-lg"
                footer={
                    <div className="flex gap-2">
                        <button
                            onClick={() => setShowForm(false)}
                            className="px-6 py-2 rounded-xl border font-medium text-gray-600 hover:bg-gray-50 transition-all"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={saveClass}
                            disabled={isSaving}
                            className="px-8 py-2 rounded-xl bg-blue-600 text-white font-bold shadow-lg hover:bg-blue-700 disabled:bg-gray-400 transition-all"
                        >
                            {isSaving ? "Saving..." : "Save Class"}
                        </button>
                    </div>
                }
            >
                <div className="grid grid-cols-2 gap-5">
                    <div className="col-span-2 space-y-4">
                        <div>
                            <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Class Name *</label>
                            <input
                                name="className"
                                placeholder="e.g. Grade 10"
                                value={form.className}
                                onChange={updateField}
                                className="input-ref"
                            />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Section *</label>
                                <input
                                    name="section"
                                    placeholder="A, B, C..."
                                    value={form.section}
                                    onChange={updateField}
                                    className="input-ref text-center font-bold"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Stream</label>
                                <input
                                    name="stream"
                                    placeholder="Science, Arts..."
                                    value={form.stream}
                                    onChange={updateField}
                                    className="input-ref"
                                />
                            </div>
                        </div>

                        <div>
                            <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Academic Session *</label>
                            <SessionSelect
                                value={form.session}
                                onChange={(val) => setForm({ ...form, session: val })}
                                className="input-ref"
                                placeholder="Select Session Year"
                            />
                        </div>

                        <label className="flex items-center gap-3 cursor-pointer p-3 bg-gray-50 rounded-xl border border-dashed border-gray-300">
                            <input
                                type="checkbox"
                                name="active"
                                checked={form.active}
                                onChange={updateField}
                                className="w-5 h-5 text-blue-600 rounded"
                            />
                            <span className="text-sm font-bold text-gray-700">Set as Active Class</span>
                        </label>
                    </div>
                </div>
            </Modal>
        </div>
    );
}
