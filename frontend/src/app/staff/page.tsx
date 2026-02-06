"use client";

import { useEffect, useState } from "react";
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

const ROLES = ["SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT", "STUDENT", "PARENT"];

export default function StaffPage() {
    const { user: currentUser } = useAuth();
    const { showToast } = useToast();
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [showForm, setShowForm] = useState(false);
    const [editId, setEditId] = useState<number | null>(null);

    const [form, setForm] = useState({
        email: "",
        fullName: "",
        password: "",
        role: "TEACHER",
        active: true,
    });

    const roles = currentUser?.role === "SUPER_ADMIN"
        ? ["PLATFORM_ADMIN", ...ROLES]
        : ROLES;

    useEffect(() => {
        loadUsers();
    }, []);

    async function loadUsers() {
        try {
            setLoading(true);
            const res = await api.get("/api/users?size=100");
            setUsers(res.data.content || []);
        } catch {
            showToast("Failed to load staff records", "error");
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

    async function saveUser() {
        if (!form.email || !form.fullName) {
            showToast("Required fields missing", "warning");
            return;
        }
        if (!editId && !form.password) {
            showToast("Password required for new accounts", "warning");
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
        } catch (e: any) {
            showToast("Operation failed: " + (e.response?.data?.message || e.message), "error");
        } finally {
            setIsSaving(false);
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center text-wrap">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Staff & Faculty</h1>
                    <p className="text-gray-500">Manage institutional users, roles, and access credentials.</p>
                </div>
                <button
                    onClick={() => {
                        resetForm();
                        setEditId(null);
                        setShowForm(true);
                    }}
                    className="bg-blue-600 text-white px-6 py-2.5 rounded-xl font-bold shadow-lg hover:bg-blue-700 transition-all flex items-center gap-2"
                >
                    <span className="text-xl">+</span> Invite Staff
                </button>
            </div>

            {loading ? (
                <div className="bg-white p-8 rounded-2xl border">
                    <TableSkeleton rows={10} cols={5} />
                </div>
            ) : (
                <div className="bg-white border rounded-2xl shadow-sm overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-gray-50 text-gray-600 font-bold border-b">
                            <tr>
                                <th className="p-4 text-left">Full Name</th>
                                <th className="p-4 text-left">Email Identity</th>
                                <th className="p-4 text-center">Assigned Role</th>
                                <th className="p-4 text-center">Status</th>
                                <th className="p-4 text-center w-24">Actions</th>
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
                                        <button
                                            onClick={() => openEdit(u)}
                                            className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-all"
                                        >
                                            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-5M16.5 3.5a2.121 2.121 0 113 3L11.707 15.364a2 2 0 01-.88.524l-4 1a1 1 0 01-1.213-1.213l1-4a2 2 0 01.524-.88L16.5 3.5z" />
                                            </svg>
                                        </button>
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
                            className="px-6 py-2 rounded-xl border font-medium text-gray-600 hover:bg-gray-50 transition-all"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={saveUser}
                            disabled={isSaving}
                            className="px-8 py-2 rounded-xl bg-blue-600 text-white font-bold shadow-lg hover:bg-blue-700 disabled:bg-gray-400 transition-all"
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
                            <label className="flex items-center gap-3 cursor-pointer p-2.5 bg-gray-50 rounded-xl border border-dashed border-gray-300 w-full">
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
        </div>
    );
}
