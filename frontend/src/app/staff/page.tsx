"use client";

import { ChangeEvent, useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/components/ui/Toast";
import Modal from "@/components/ui/Modal";
import { TableSkeleton } from "@/components/ui/Skeleton";

type User = {
    id: number;
    email: string;
    fullName: string;
    role: string;
    active: boolean;
};

type TeacherProfileData = {
    id: number;
    userId: number;
    fullName: string;
    assignments: Array<{
        id: number;
        className: string;
        subjectName: string;
        sessionName: string;
        status: string;
    }>;
    classes: Array<{
        id: number;
        name: string;
        section?: string;
    }>;
};

const ROLES = ["SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT", "STUDENT", "PARENT"];

export default function StaffPage() {
    const { user: currentUser } = useAuth();
    const { showToast } = useToast();
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [showForm, setShowForm] = useState(false);
    const [editId, setEditId] = useState<number | null>(null);

    // Teacher Profile Modal State
    const [showProfile, setShowProfile] = useState(false);
    const [profileLoading, setProfileLoading] = useState(false);
    const [teacherProfile, setTeacherProfile] = useState<TeacherProfileData | null>(null);

    const [form, setForm] = useState({
        email: "",
        fullName: "",
        password: "",
        role: "TEACHER",
        active: true,
    });

    const currentRole = currentUser?.role?.toUpperCase();
    const isHighLevelAdmin = currentRole === "SUPER_ADMIN" || currentRole === "PLATFORM_ADMIN";

    const roles = isHighLevelAdmin
        ? ["PLATFORM_ADMIN", ...ROLES]
        : ROLES;

    function getErrorMessage(error: unknown): string {
        if (error && typeof error === "object" && "response" in error) {
            const response = (error as { response?: { data?: { message?: string } } }).response;
            if (response?.data?.message) return response.data.message;
        }
        if (error instanceof Error) return error.message;
        return "Unknown error";
    }

    const loadUsers = useCallback(async () => {
        try {
            setLoading(true);
            const res = await api.get("/api/users?size=100");
            setUsers(res.data.content || []);
        } catch (e: unknown) {
            const msg = getErrorMessage(e);
            showToast("Failed to load staff: " + msg, "error");
        } finally {
            setLoading(false);
        }
    }, [showToast]);

    useEffect(() => {
        loadUsers();
    }, [loadUsers]);

    function updateField(e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
        const value = e.target.type === "checkbox" ? e.target.checked : e.target.value;
        setForm({ ...form, [e.target.name]: value });
    }

    function resetForm() {
        setForm({
            email: "",
            fullName: "",
            password: "",
            role: "TEACHER",
            active: true,
        });
    }

    function openEdit(u: User) {
        setForm({
            email: u.email,
            fullName: u.fullName || "",
            password: "",
            role: u.role,
            active: u.active,
        });
        setEditId(u.id);
        setShowForm(true);
    }

    async function viewTeacherProfile(u: User) {
        try {
            setProfileLoading(true);
            setShowProfile(true);

            // 1. Fetch Teacher entity details linked to User
            const teacherRes = await api.get(`/api/teachers/by-user/${u.id}`);
            const teacher = teacherRes.data;

            // 2. Fetch Assignments and Class maps concurrently
            const currentSessionId = localStorage.getItem("currentSessionId") || "";
            const [assignmentsRes, classesRes] = await Promise.all([
                api.get(`/api/class-subjects/assignments?teacherId=${teacher.id}${currentSessionId ? `&sessionId=${currentSessionId}` : ''}`),
                api.get("/api/classes")
            ]);

            const assignedClassesIds = new Set(assignmentsRes.data.map((a: any) => a.className)); // or filtered explicitly if class payload is exposed

            setTeacherProfile({
                id: teacher.id,
                userId: teacher.userId,
                fullName: teacher.fullName,
                assignments: assignmentsRes.data || [],
                classes: classesRes.data.content
                    ? classesRes.data.content.filter((c: any) => c.classTeacherId === teacher.id)
                    : []
            });
        } catch (e) {
            showToast("Failed to load teacher profile", "error");
            setShowProfile(false);
        } finally {
            setProfileLoading(false);
        }
    }

    async function saveUser() {
        if (!form.email || !form.fullName) {
            showToast("Required fields missing", "warning");
            return;
        }
        if (!editId && !form.password) {
            showToast("Password required for new accounts", "warning");
            return;
        }
        // Guard staff creation for platform roles without school selection
        const selectedSchoolId = localStorage.getItem("schoolId");
        if (["SUPER_ADMIN", "PLATFORM_ADMIN"].includes(currentUser?.role?.toUpperCase() ?? "") && !selectedSchoolId) {
            showToast("Please select a school first from the Schools page.", "warning");
            return;
        }

        try {
            setIsSaving(true);
            if (editId) {
                await api.put(`/api/users/${editId}`, form);
                showToast("Staff record updated", "success");
            } else {
                await api.post("/api/users", form);
                showToast("New staff account created!", "success");
            }
            setShowForm(false);
            setEditId(null);
            resetForm();
            loadUsers();
        } catch (e: unknown) {
            showToast("Operation failed: " + getErrorMessage(e), "error");
        } finally {
            setIsSaving(false);
        }
    }

    return (
        <div className="mx-auto px-6 py-6 space-y-6">
            <div className="flex justify-between items-center text-wrap">
                <div>
                    <h1 className="text-lg font-semibold">Staff & Faculty</h1>
                    <p className="text-gray-500 text-base mt-1">Manage institutional users, roles, and access credentials.</p>
                </div>
                <button
                    onClick={() => {
                        resetForm();
                        setEditId(null);
                        setShowForm(true);
                    }}
                    className="bg-blue-600 text-white px-6 py-2.5 rounded-md font-medium hover:bg-blue-700 flex items-center gap-2 text-base"
                >
                    <span className="text-xl">+</span> Invite Staff
                </button>
            </div>

            {loading ? (
                <div className="bg-white rounded-lg shadow border border-gray-100 p-6">
                    <TableSkeleton rows={10} cols={5} />
                </div>
            ) : (
                <div className="bg-white rounded-lg shadow border border-gray-100 overflow-hidden mt-4">
                    <table className="w-full text-base">
                        <thead className="bg-gray-50 text-gray-600 text-lg font-semibold border-b border-gray-100">
                            <tr>
                                <th className="px-6 py-4 text-left">Full Name</th>
                                <th className="px-6 py-4 text-left">Email Identity</th>
                                <th className="px-6 py-4 text-center">Assigned Role</th>
                                <th className="px-6 py-4 text-center">Status</th>
                                <th className="px-6 py-4 text-center w-24">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {users.map((u) => (
                                <tr key={u.id} className="hover:bg-gray-50/50 transition-colors">
                                    <td className="p-4 font-bold text-gray-800">{u.fullName}</td>
                                    <td className="p-4 text-gray-500 italic lowercase">{u.email}</td>
                                    <td className="p-4 text-center">
                                        <span className="px-3 py-1 bg-gray-100 text-gray-700 rounded-lg text-[10px] font-black uppercase tracking-tight border">
                                            {u.role.replace('_', ' ')}
                                        </span>
                                    </td>
                                    <td className="p-4 text-center">
                                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${u.active ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"
                                            }`}>
                                            {u.active ? "Active" : "Suspended"}
                                        </span>
                                    </td>
                                    <td className="p-4 text-center">
                                        <div className="flex justify-center gap-2">
                                            {u.role === 'TEACHER' && (
                                                <button
                                                    onClick={() => viewTeacherProfile(u)}
                                                    className="p-2 text-indigo-600 hover:bg-indigo-50 rounded-md"
                                                    title="View Teacher Profile"
                                                >
                                                    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                                    </svg>
                                                </button>
                                            )}
                                            <button
                                                onClick={() => openEdit(u)}
                                                className="p-2 text-blue-600 hover:bg-blue-50 rounded-md"
                                                title="Edit Staff User"
                                            >
                                                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-5M16.5 3.5a2.121 2.121 0 113 3L11.707 15.364a2 2 0 01-.88.524l-4 1a1 1 0 01-1.213-1.213l1-4a2 2 0 01.524-.88L16.5 3.5z" />
                                                </svg>
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <Modal
                isOpen={showForm}
                onClose={() => setShowForm(false)}
                title={editId ? "Update Staff Profile" : "Create New Staff Account"}
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
                            onClick={saveUser}
                            disabled={isSaving}
                            className="px-8 py-2 rounded-md bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50"
                        >
                            {isSaving ? "Processing..." : "Commit User Data"}
                        </button>
                    </div>
                }
            >
                <div className="space-y-5">
                    <div>
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Display Name *</label>
                        <input
                            name="fullName"
                            placeholder="Full Name"
                            value={form.fullName}
                            onChange={updateField}
                            className="input-ref"
                        />
                    </div>

                    <div>
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Email Address *</label>
                        <input
                            name="email"
                            placeholder="staff@school.com"
                            value={form.email}
                            onChange={updateField}
                            className="input-ref lowercase"
                        />
                    </div>

                    <div>
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Credentials *</label>
                        <input
                            name="password"
                            type="password"
                            placeholder={editId ? "•••••••• (Keep empty to retain)" : "Temporary Password"}
                            value={form.password}
                            onChange={updateField}
                            className="input-ref font-mono"
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Access Role *</label>
                            <select
                                name="role"
                                value={form.role}
                                onChange={updateField}
                                className="input-ref font-bold"
                            >
                                {roles.map(r => (
                                    <option key={r} value={r}>{r.replace('_', ' ')}</option>
                                ))}
                            </select>
                        </div>
                        <div className="flex items-end">
                            <label className="flex items-center gap-3 cursor-pointer p-4 rounded-lg border border-gray-100 bg-gray-50 w-full">
                                <input
                                    type="checkbox"
                                    name="active"
                                    checked={form.active}
                                    onChange={updateField}
                                    className="w-5 h-5 text-blue-600 rounded"
                                />
                                <span className="text-sm font-bold text-gray-700">Account Active</span>
                            </label>
                        </div>
                    </div>
                </div>
            </Modal>

            {/* Teacher Profile Modal */}
            <Modal
                isOpen={showProfile}
                onClose={() => setShowProfile(false)}
                title="Teacher Profile"
                maxWidth="max-w-3xl"
            >
                {profileLoading ? (
                    <div className="space-y-4">
                        <TableSkeleton rows={3} cols={1} />
                    </div>
                ) : teacherProfile ? (
                    <div className="space-y-6">
                        {/* Header Details */}
                        <div className="flex items-center gap-4 bg-gray-50 p-4 rounded-xl border border-gray-100">
                            <div className="w-16 h-16 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-2xl font-bold">
                                {teacherProfile.fullName.charAt(0)}
                            </div>
                            <div>
                                <h3 className="text-xl font-bold text-gray-900">{teacherProfile.fullName}</h3>
                                <p className="text-sm text-gray-500 font-mono">Teacher ID: {teacherProfile.id}</p>
                            </div>
                        </div>

                        {/* Class Teacher Assignments */}
                        <div className="space-y-3">
                            <h4 className="text-sm font-bold text-gray-700 uppercase tracking-widest border-b pb-2">Class Teacher Roles</h4>
                            {teacherProfile.classes.length === 0 ? (
                                <p className="text-sm text-gray-500 italic">Not assigned as class teacher to any classes.</p>
                            ) : (
                                <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                                    {teacherProfile.classes.map(c => (
                                        <div key={c.id} className="bg-white p-3 border rounded-xl shadow-sm flex items-center gap-3">
                                            <div className="bg-indigo-100 text-indigo-700 p-2 rounded-lg">
                                                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                                                </svg>
                                            </div>
                                            <div>
                                                <div className="font-bold text-gray-800 text-sm">{c.name} {c.section && `- ${c.section}`}</div>
                                                <div className="text-[10px] text-gray-500 uppercase tracking-wider">Class Teacher</div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>

                        {/* Subject Assignments */}
                        <div className="space-y-3">
                            <h4 className="text-sm font-bold text-gray-700 uppercase tracking-widest border-b pb-2">Subject Assignments</h4>
                            {teacherProfile.assignments.length === 0 ? (
                                <p className="text-sm text-gray-500 italic">No subjects assigned for the current session.</p>
                            ) : (
                                <div className="bg-white border rounded-xl overflow-hidden shadow-sm">
                                    <table className="w-full text-sm">
                                        <thead className="bg-gray-50 border-b">
                                            <tr>
                                                <th className="px-4 py-3 text-left font-bold text-gray-600 uppercase text-[10px] tracking-wider">Class</th>
                                                <th className="px-4 py-3 text-left font-bold text-gray-600 uppercase text-[10px] tracking-wider">Subject</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-gray-100">
                                            {teacherProfile.assignments.map(a => (
                                                <tr key={a.id} className="hover:bg-gray-50/50">
                                                    <td className="px-4 py-3 font-medium text-gray-900">{a.className}</td>
                                                    <td className="px-4 py-3 text-gray-600">{a.subjectName}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </div>
                ) : (
                    <div className="py-8 text-center text-gray-500">
                        Profile could not be loaded.
                    </div>
                )}
            </Modal>
        </div>
    );
}
