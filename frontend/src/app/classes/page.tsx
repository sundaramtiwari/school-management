"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";

type SchoolClass = {
    id: number;
    className: string;
    section: string;
    stream?: string;
    session: string;
    active: boolean;
};

export default function ClassesPage() {
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [showForm, setShowForm] = useState(false);
    const [editId, setEditId] = useState<number | null>(null);

    const [form, setForm] = useState({
        className: "",
        section: "A",
        stream: "",
        session: "2024-25", // Default, could be dynamic
        active: true,
    });

    useEffect(() => {
        loadClasses();
    }, []);

    async function loadClasses() {
        try {
            setLoading(true);
            const res = await api.get("/api/classes?size=100");
            setClasses(res.data.content || []);
        } catch {
            setError("Failed to load classes");
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
            className: c.className,
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
            alert("Name, Section and Session are required");
            return;
        }

        try {
            if (editId) {
                await api.put(`/api/classes/${editId}`, form);
            } else {
                await api.post("/api/classes", form);
            }
            setShowForm(false);
            setEditId(null);
            resetForm();
            loadClasses();
        } catch (e: any) {
            alert("Save failed: " + (e.response?.data?.message || e.message));
        }
    }

    async function deleteClass(id: number) {
        if (!confirm("Are you sure? This might affect students linked to this class.")) return;
        try {
            await api.delete(`/api/classes/${id}`);
            loadClasses();
        } catch (e: any) {
            alert("Delete failed");
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold">Classes</h1>
                <button
                    onClick={() => {
                        resetForm();
                        setEditId(null);
                        setShowForm(true);
                    }}
                    className="bg-blue-600 text-white px-4 py-2 rounded"
                >
                    + Add Class
                </button>
            </div>

            {loading && <p>Loading...</p>}
            {error && <p className="text-red-500">{error}</p>}

            {!loading && !error && (
                <div className="bg-white border rounded">
                    <table className="w-full text-sm">
                        <thead className="bg-gray-100">
                            <tr>
                                <th className="p-3 text-left">Class Name</th>
                                <th className="p-3 text-center">Section</th>
                                <th className="p-3 text-center">Stream</th>
                                <th className="p-3 text-center">Session</th>
                                <th className="p-3 text-center">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {classes.map((c) => (
                                <tr key={c.id} className="border-t hover:bg-gray-50">
                                    <td className="p-3">{c.className}</td>
                                    <td className="p-3 text-center">{c.section}</td>
                                    <td className="p-3 text-center">{c.stream || "-"}</td>
                                    <td className="p-3 text-center">{c.session}</td>
                                    <td className="p-3 text-center space-x-2">
                                        <button
                                            onClick={() => openEdit(c)}
                                            className="text-blue-600 hover:text-blue-800"
                                        >
                                            Edit
                                        </button>
                                        <button
                                            onClick={() => deleteClass(c.id)}
                                            className="text-red-600 hover:text-red-800"
                                        >
                                            Delete
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
                            {editId ? "Edit Class" : "Add Class"}
                        </h2>

                        <div className="grid grid-cols-2 gap-4">
                            <input
                                name="className"
                                placeholder="Class Name (e.g. X) *"
                                value={form.className}
                                onChange={updateField}
                                className="input"
                            />

                            <input
                                name="section"
                                placeholder="Section (e.g. A) *"
                                value={form.section}
                                onChange={updateField}
                                className="input"
                            />

                            <input
                                name="stream"
                                placeholder="Stream (Science/Arts)"
                                value={form.stream}
                                onChange={updateField}
                                className="input"
                            />

                            <input
                                name="session"
                                placeholder="Session (2024-25) *"
                                value={form.session}
                                onChange={updateField}
                                className="input"
                            />
                        </div>

                        <div className="pt-4 flex justify-end gap-2">
                            <button
                                onClick={() => setShowForm(false)}
                                className="text-gray-500"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={saveClass}
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
