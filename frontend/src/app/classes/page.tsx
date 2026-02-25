"use client";

import Link from "next/link";

import { useCallback, useEffect, useState, type ChangeEvent } from "react";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";
import { useAuth } from "@/context/AuthContext";
import { useSession } from "@/context/SessionContext";
import Modal from "@/components/ui/Modal";
import { TableSkeleton } from "@/components/ui/Skeleton";
import ClassSubjectsManager from "@/components/classes/ClassSubjectsManager";

type SchoolClass = {
    id: number;
    name: string;
    section: string;
    stream?: string;
    capacity?: number | null;
    classTeacherId?: number | null;
    remarks?: string | null;
    sessionId: number;
    active: boolean;
};

type Teacher = {
    id: number;
    fullName: string;
};

export default function ClassesPage() {
    const { user } = useAuth();
    const { showToast } = useToast();
    const { currentSession } = useSession();

    const canManageClasses = user?.role === "SCHOOL_ADMIN" || user?.role === "SUPER_ADMIN" || user?.role === "PLATFORM_ADMIN";
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [loading, setLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    const [showForm, setShowForm] = useState(false);
    const [editId, setEditId] = useState<number | null>(null);
    const [showInactive, setShowInactive] = useState(false);
    const [postCreateClass, setPostCreateClass] = useState<{ id: number; name: string } | null>(null);
    const [teachers, setTeachers] = useState<Teacher[]>([]);

    const [form, setForm] = useState({
        className: "",
        section: "A",
        stream: "",
        classTeacherId: "",
        capacity: "",
        remarks: "",
        active: true,
    });

    const loadClasses = useCallback(async () => {
        if (!user) return;
        try {
            setLoading(true);

            if (user.role === "TEACHER") {
                if (!currentSession) {
                    setClasses([]);
                    return;
                }
                const res = await api.get(`/api/classes/my-classes?includeInactive=${showInactive}`);
                setClasses(res.data.content || []);
            } else {
                const endpoint = "/api/classes/mine";
                const res = await api.get(`${endpoint}?size=100&includeInactive=${showInactive}`);
                setClasses(res.data.content || []);
            }
        } catch {
            showToast("Failed to load classes", "error");
        } finally {
            setLoading(false);
        }
    }, [showToast, user, currentSession]);

    useEffect(() => {
        void loadClasses();
    }, [currentSession, loadClasses, showInactive]);

    useEffect(() => {
        if (!canManageClasses) return;
        const loadTeachers = async () => {
            try {
                const res = await api.get<Teacher[]>("/api/teachers");
                setTeachers(res.data || []);
            } catch {
                setTeachers([]);
            }
        };
        void loadTeachers();
    }, [canManageClasses]);

    function updateField(e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) {
        const value = e.target.type === "checkbox" ? e.target.checked : e.target.value;
        setForm({ ...form, [e.target.name]: value });
    }

    function resetForm() {
        setForm({
            className: "",
            section: "A",
            stream: "",
            classTeacherId: "",
            capacity: "",
            remarks: "",
            active: true,
        });
    }

    function openEdit(c: SchoolClass) {
        setForm({
            className: c.name,
            section: c.section,
            stream: c.stream || "",
            classTeacherId: c.classTeacherId ? String(c.classTeacherId) : "",
            capacity: c.capacity != null ? String(c.capacity) : "",
            remarks: c.remarks || "",
            active: c.active,
        });
        setEditId(c.id);
        setShowForm(true);
    }

    async function saveClass() {
        if (!form.className || !form.section || !currentSession) {
            showToast(currentSession ? "Required fields missing" : "No active session selected", "warning");
            return;
        }

        try {
            setIsSaving(true);
            const payload = {
                name: form.className,
                section: form.section,
                stream: form.stream || null,
                sessionId: currentSession.id,
                classTeacherId: form.classTeacherId ? Number(form.classTeacherId) : null,
                capacity: form.capacity === "" ? null : Number(form.capacity),
                remarks: form.remarks?.trim() ? form.remarks.trim() : null,
                active: form.active,
            };

            if (editId) {
                await api.put(`/api/classes/${editId}`, payload);
                showToast("Class updated successfully!", "success");
                setShowForm(false);
                setEditId(null);
                resetForm();
                void loadClasses();
            } else {
                const res = await api.post("/api/classes", payload);
                showToast("Class created! Now assign subjects.", "success");
                setShowForm(false);
                setEditId(null);
                resetForm();
                void loadClasses();

                setTimeout(() => {
                    setPostCreateClass({ id: res.data.id, name: res.data.name });
                }, 150);
            }
        } catch (e: unknown) {
            const message = typeof e === "object" && e !== null && "message" in e
                ? String((e as { message?: string }).message || "Unknown error")
                : "Unknown error";
            showToast("Save failed: " + message, "error");
        } finally {
            setIsSaving(false);
        }
    }

    async function deleteClass(id: number) {
        if (!confirm("Are you sure? This might affect students linked to this class.")) return;
        try {
            setIsDeleting(true);
            showToast("Deleting class...", "info");
            await api.delete(`/api/classes/${id}`);
            showToast("Class deleted successfully", "success");
            void loadClasses();
        } catch (error: any) {
            showToast("Delete failed: " + (error.response?.data?.message || error.message), "error");
        } finally {
            setIsDeleting(false);
        }
    }

    async function handleToggle(c: SchoolClass) {
        const action = c.active ? "deactivate" : "reactivate";
        if (!confirm(`Are you sure you want to ${action} "${c.name}"?`)) return;

        try {
            await api.patch(`/api/classes/${c.id}/toggle`);
            showToast(`Class ${action}d successfully`, "success");
            void loadClasses();
        } catch (error: any) {
            showToast(`Failed to ${action} class: ` + (error.response?.data?.message || error.message), "error");
        }
    }

    return (
        <div className="mx-auto px-6 py-6 space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-lg font-semibold">Class Management</h1>
                    <p className="text-gray-500 text-base mt-1">Define and organize academic classes for <span className="text-blue-600 font-bold">{currentSession?.name || "current session"}</span>.</p>
                </div>
                <div className="flex items-center gap-4">
                    <label className="flex items-center gap-2 cursor-pointer bg-white border border-gray-200 px-4 py-2 rounded-md shadow-sm">
                        <input
                            type="checkbox"
                            checked={showInactive}
                            onChange={(e) => setShowInactive(e.target.checked)}
                            className="w-4 h-4 text-blue-600 rounded"
                        />
                        <span className="text-sm font-medium text-gray-700">Show Inactive</span>
                    </label>
                    {canManageClasses && (
                        <button
                            onClick={() => {
                                resetForm();
                                setEditId(null);
                                setShowForm(true);
                            }}
                            className="bg-blue-600 text-white px-6 py-2.5 rounded-md font-medium hover:bg-blue-700 flex items-center gap-2 text-base"
                        >
                            <span className="text-xl">+</span> Add Class
                        </button>
                    )}
                </div>
            </div>

            {loading ? (
                <div className="bg-white rounded-lg shadow border border-gray-100 p-6">
                    <TableSkeleton rows={8} cols={4} />
                </div>
            ) : (
                <div className="bg-white rounded-lg shadow border border-gray-100 overflow-hidden mt-4">
                    <table className="w-full text-base">
                        <thead className="bg-gray-50 text-gray-600 text-lg font-semibold border-b border-gray-100">
                            <tr>
                                <th className="px-6 py-4 text-left">Class Name</th>
                                <th className="px-6 py-4 text-center">Section</th>
                                <th className="px-6 py-4 text-center">Stream</th>
                                <th className="px-6 py-4 text-center">Status</th>
                                <th className="px-6 py-4 text-center w-48">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {classes.map((c) => (
                                <tr key={c.id} className="hover:bg-gray-50/50 transition-colors">
                                    <td className="p-4 font-bold text-gray-800">
                                        <Link href={`/classes/${c.id}`} className="hover:text-blue-600 hover:underline">
                                            {c.name}
                                        </Link>
                                    </td>
                                    <td className="p-4 text-center">
                                        <span className="px-2 py-1 bg-gray-100 rounded text-xs font-bold">{c.section}</span>
                                    </td>
                                    <td className="p-4 text-center text-gray-500 uppercase text-xs">{c.stream || "-"}</td>
                                    <td className="p-4 text-center">
                                        <span className={`px-2 py-1 rounded text-[10px] uppercase font-bold ${c.active ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                                            {c.active ? "Active" : "Inactive"}
                                        </span>
                                    </td>
                                    <td className="p-4 text-center">
                                        {canManageClasses ? (
                                            <div className="flex justify-center gap-2">
                                                <button
                                                    onClick={() => openEdit(c)}
                                                    className="p-2 text-blue-600 hover:bg-blue-50 rounded-md"
                                                    title="Edit Class"
                                                >
                                                    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-5M16.5 3.5a2.121 2.121 0 113 3L11.707 15.364a2 2 0 01-.88.524l-4 1a1 1 0 01-1.213-1.213l1-4a2 2 0 01.524-.88L16.5 3.5z" />
                                                    </svg>
                                                </button>
                                                <button
                                                    onClick={() => handleToggle(c)}
                                                    className={`p-2 rounded-md ${c.active ? 'text-orange-600 hover:bg-orange-50' : 'text-emerald-600 hover:bg-emerald-50'}`}
                                                    title={c.active ? "Deactivate Class" : "Reactivate Class"}
                                                >
                                                    {c.active ? (
                                                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                                                        </svg>
                                                    ) : (
                                                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                                        </svg>
                                                    )}
                                                </button>
                                                <button
                                                    onClick={() => deleteClass(c.id)}
                                                    disabled={isDeleting}
                                                    className="p-2 text-red-600 hover:bg-red-50 rounded-md disabled:opacity-30"
                                                    title="Permanently Deactivate (Soft Delete)"
                                                >
                                                    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                    </svg>
                                                </button>
                                            </div>
                                        ) : (
                                            <span className="text-gray-300 text-xs italic">Read Only</span>
                                        )}
                                    </td>
                                </tr>
                            ))}
                            {classes.length === 0 && (
                                <tr>
                                    <td colSpan={4} className="p-20 text-center text-gray-400 italic bg-gray-50/30">
                                        <div className="flex flex-col items-center gap-3">
                                            <span className="text-4xl">🏫</span>
                                            <div>
                                                <p className="font-bold text-gray-800">No Classes Defined</p>
                                                <p className="text-sm">You haven&apos;t added any classes for this session yet.</p>
                                            </div>
                                            {canManageClasses && (
                                                <button
                                                    onClick={() => setShowForm(true)}
                                                    className="mt-2 bg-blue-600 text-white px-6 py-2 rounded-md font-medium hover:bg-blue-700"
                                                >
                                                    Add First Class →
                                                </button>
                                            )}
                                        </div>
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
                            className="px-6 py-2 rounded-md bg-white border border-gray-300 font-medium text-gray-600 hover:bg-gray-50"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={saveClass}
                            disabled={isSaving}
                            className="px-8 py-2 rounded-md bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50"
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

                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Class Teacher</label>
                                <select
                                    name="classTeacherId"
                                    value={form.classTeacherId}
                                    onChange={updateField}
                                    className="input-ref"
                                >
                                    <option value="">Select teacher</option>
                                    {teachers.map((teacher) => (
                                        <option key={teacher.id} value={teacher.id}>
                                            {teacher.fullName}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Capacity</label>
                                <input
                                    type="number"
                                    min={0}
                                    name="capacity"
                                    placeholder="e.g. 40"
                                    value={form.capacity}
                                    onChange={updateField}
                                    className="input-ref"
                                />
                            </div>
                        </div>

                        <div>
                            <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Remarks</label>
                            <textarea
                                name="remarks"
                                placeholder="Optional notes for this class"
                                value={form.remarks}
                                onChange={updateField}
                                rows={3}
                                className="input-ref"
                            />
                        </div>

                        <div className="p-4 rounded-lg border border-gray-100 bg-gray-50 text-gray-500 text-base italic">
                            This class will be associated with the active session: <span className="font-bold">{currentSession?.name || "None"}</span>
                        </div>

                        <label className="flex items-center gap-3 cursor-pointer p-4 rounded-lg border border-gray-100 bg-gray-50">
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

            <Modal
                isOpen={postCreateClass !== null}
                onClose={() => setPostCreateClass(null)}
                title={postCreateClass ? `Assign Subjects to Class: ${postCreateClass.name}` : "Assign Subjects"}
                maxWidth="max-w-4xl"
                footer={
                    <button
                        onClick={() => setPostCreateClass(null)}
                        className="px-6 py-2 rounded-md bg-blue-600 text-white font-medium hover:bg-blue-700"
                    >
                        Done Setting Subjects
                    </button>
                }
            >
                {postCreateClass && (
                    <div className="p-4 bg-gray-50 rounded-lg border border-gray-100">
                        <ClassSubjectsManager classId={postCreateClass.id} />
                    </div>
                )}
            </Modal>
        </div>
    );
}
