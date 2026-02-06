"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";

type User = {
    id: number;
    email: string;
    fullName: string;
    role: string;
    active: boolean;
};

const ROLES = ["SCHOOL_ADMIN", "TEACHER", "ACCOUNTANT", "STUDENT", "PARENT"];

export default function StaffPage() {
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [showForm, setShowForm] = useState(false);
    const [editId, setEditId] = useState<number | null>(null);

    const [form, setForm] = useState({
        email: "",
        fullName: "",
        password: "",
        role: "TEACHER",
        active: true,
    });

    useEffect(() => {
        loadUsers();
    }, []);

    async function loadUsers() {
        try {
            setLoading(true);
            // Backend /api/users returns filtered list for this school
            const res = await api.get("/api/users?size=100");
            setUsers(res.data.content || []);
        } catch {
            setError("Failed to load staff");
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
            password: "", // Don't show existing hash
            role: u.role,
            active: u.active,
        });
        setEditId(u.id);
        setShowForm(true);
    }

    async function saveUser() {
        if (!form.email || !form.fullName) {
            alert("Email and Name are required");
            return;
        }
        if (!editId && !form.password) {
            alert("Password required for new users");
            return;
        }

        try {
            if (editId) {
                await api.put(`/api/users/${editId}`, form);
            } else {
                await api.post("/api/users", form);
            }
            setShowForm(false);
            setEditId(null);
            resetForm();
            loadUsers();
        } catch (e: any) {
            alert("Save failed: " + (e.response?.data?.message || e.message));
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold">Staff Management</h1>
                <button
                    onClick={() => {
                        resetForm();
                        setEditId(null);
                        setShowForm(true);
                    }}
                    className="bg-blue-600 text-white px-4 py-2 rounded"
                >
                    + Add Staff
                </button>
            </div>

            {loading && <p>Loading...</p>}
            {error && <p className="text-red-500">{error}</p>}

            {!loading && !error && (
                <div className="bg-white border rounded">
                    <table className="w-full text-sm">
                        <thead className="bg-gray-100">
                            <tr>
                                <th className="p-3 text-left">Name</th>
                                <th className="p-3 text-left">Email</th>
                                <th className="p-3 text-center">Role</th>
                                <th className="p-3 text-center">Status</th>
                                <th className="p-3 text-center">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {users.map((u) => (
                                <tr key={u.id} className="border-t hover:bg-gray-50">
                                    <td className="p-3">{u.fullName}</td>
                                    <td className="p-3">{u.email}</td>
                                    <td className="p-3 text-center">
                                        <span className="bg-gray-100 px-2 py-1 rounded text-xs font-semibold">
                                            {u.role}
                                        </span>
                                    </td>
                                    <td className="p-3 text-center">
                                        {u.active ? (
                                            <span className="text-green-600">Active</span>
                                        ) : (
                                            <span className="text-red-600">Inactive</span>
                                        )}
                                    </td>
                                    <td className="p-3 text-center">
                                        <button
                                            onClick={() => openEdit(u)}
                                            className="text-blue-600 hover:text-blue-800"
                                        >
                                            Edit
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {showForm && (
                <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
                    <div className="bg-white p-6 rounded w-[500px] space-y-4">
                        <h2 className="font-semibold text-lg">
                            {editId ? "Edit Staff" : "Add Staff"}
                        </h2>

                        <input
                            name="fullName"
                            placeholder="Full Name *"
                            value={form.fullName}
                            onChange={updateField}
                            className="input w-full"
                        />

                        <input
                            name="email"
                            placeholder="Email *"
                            value={form.email}
                            onChange={updateField}
                            className="input w-full"
                        />

                        <input
                            name="password"
                            type="password"
                            placeholder={editId ? "Password (leave empty to keep)" : "Password *"}
                            value={form.password}
                            onChange={updateField}
                            className="input w-full"
                        />

                        <select
                            name="role"
                            value={form.role}
                            onChange={updateField}
                            className="input w-full"
                        >
                            {ROLES.map(r => (
                                <option key={r} value={r}>{r}</option>
                            ))}
                        </select>

                        <label className="flex items-center gap-2">
                            <input
                                type="checkbox"
                                name="active"
                                checked={form.active}
                                onChange={updateField}
                            />
                            Active User
                        </label>

                        <div className="pt-4 flex justify-end gap-2">
                            <button
                                onClick={() => setShowForm(false)}
                                className="text-gray-500"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={saveUser}
                                className="bg-blue-600 text-white px-6 py-2 rounded"
                            >
                                Save
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
