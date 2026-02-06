"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";

type FeeStructure = {
    id: number;
    feeTypeName: string;
    amount: number;
    frequency: string;
    active: boolean;
};

type SchoolClass = {
    id: number;
    name: string;
    section: string;
    session: string;
};

const FREQUENCIES = ["ONE_TIME", "MONTHLY", "ANNUALLY"];

export default function FeeStructuresPage() {

    /* -------- State -------- */
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [selectedClass, setSelectedClass] = useState<number | "">("");
    const [structures, setStructures] = useState<FeeStructure[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadingClasses, setLoadingClasses] = useState(true);

    const [showModal, setShowModal] = useState(false);
    const [feeTypes, setFeeTypes] = useState<any[]>([]); // Need to load fee types

    const [form, setForm] = useState({
        feeTypeId: "",
        amount: "",
        frequency: "ONE_TIME",
        session: "2024-25", // Should match Class Session
    });

    /* -------- Loaders -------- */

    useEffect(() => {
        loadClasses();
        loadFeeTypes();
    }, []);

    async function loadClasses() {
        try {
            setLoadingClasses(true);
            const res = await api.get("/api/classes?size=100");
            setClasses(res.data.content || []);
        } catch (e) {
            console.error(e);
        } finally {
            setLoadingClasses(false);
        }
    }

    async function loadFeeTypes() {
        // Assuming we have an endpoint for fee types. If not, we might need to create one or hardcode
        // For now, let's assume /api/fees/types exists ?
        // Wait, check Backend. FeeTypeController exists?
        try {
            const res = await api.get("/api/fees/types");
            setFeeTypes(res.data || []);
        } catch {
            // Fallback or empty
        }
    }

    /* -------- Handlers -------- */

    async function onClassChange(e: any) {
        const classId = e.target.value;
        setSelectedClass(classId);
        setStructures([]);

        if (classId) {
            loadStructures(classId);
        }
    }

    async function loadStructures(classId: number) {
        try {
            setLoading(true);
            const cls = classes.find(c => c.id == classId);
            const session = cls ? cls.session : "2024-25";

            // Endpoint: /api/fees/structures/by-class/{id}?session={session}
            const res = await api.get(`/api/fees/structures/by-class/${classId}?session=${session}`);
            setStructures(res.data || []);
            setForm(f => ({ ...f, session })); // Sync session
        } catch (e) {
            alert("Failed to load structures");
        } finally {
            setLoading(false);
        }
    }

    async function saveStructure() {
        if (!selectedClass) return;
        if (!form.feeTypeId || !form.amount) {
            alert("All fields required");
            return;
        }

        try {
            await api.post("/api/fees/structures", {
                classId: selectedClass,
                session: form.session,
                feeTypeId: form.feeTypeId,
                amount: Number(form.amount),
                frequency: form.frequency
            });
            setShowModal(false);
            loadStructures(Number(selectedClass));
        } catch (e: any) {
            alert("Save failed: " + (e.response?.data?.message || e.message));
        }
    }

    /* -------- UI -------- */

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h2 className="text-xl font-bold">Fee Structures</h2>
                <button
                    disabled={!selectedClass}
                    onClick={() => setShowModal(true)}
                    className="bg-blue-600 text-white px-4 py-2 rounded disabled:bg-gray-300"
                >
                    + Add Fee
                </button>
            </div>

            {/* Class Selector */}
            <div className="w-1/3">
                <label className="block text-sm font-medium mb-1">Select Class</label>
                <select
                    className="input w-full"
                    value={selectedClass}
                    onChange={onClassChange}
                    disabled={loadingClasses}
                >
                    <option value="">Select...</option>
                    {classes.map(c => (
                        <option key={c.id} value={c.id}>{c.name} {c.section} ({c.session})</option>
                    ))}
                </select>
            </div>

            {loading && <p>Loading structures...</p>}

            {selectedClass && !loading && (
                <div className="bg-white border rounded">
                    <table className="w-full text-sm">
                        <thead className="bg-gray-100">
                            <tr>
                                <th className="p-3 text-left">Fee Head</th>
                                <th className="p-3 text-center">Frequency</th>
                                <th className="p-3 text-right">Amount (₹)</th>
                                <th className="p-3 text-center">Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            {structures.map(fs => (
                                <tr key={fs.id} className="border-t">
                                    <td className="p-3 font-medium">{fs.feeTypeName}</td>
                                    <td className="p-3 text-center">
                                        <span className="px-2 py-1 bg-gray-100 rounded text-xs">
                                            {fs.frequency}
                                        </span>
                                    </td>
                                    <td className="p-3 text-right font-mono">
                                        {fs.amount.toLocaleString("en-IN")}
                                    </td>
                                    <td className="p-3 text-center text-green-600">
                                        {fs.active ? "Active" : "Inactive"}
                                    </td>
                                </tr>
                            ))}
                            {structures.length === 0 && (
                                <tr><td colSpan={4} className="p-4 text-center text-gray-400">No fees defined</td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
                    <div className="bg-white p-6 rounded w-[450px] space-y-4">
                        <h3 className="font-bold text-lg">Add Fee Structure</h3>

                        <div>
                            <label className="block text-sm mb-1">Fee Head</label>
                            <select
                                className="input w-full"
                                value={form.feeTypeId}
                                onChange={(e) => setForm({ ...form, feeTypeId: e.target.value })}
                            >
                                <option value="">Select Head...</option>
                                {feeTypes.map(t => (
                                    <option key={t.id} value={t.id}>{t.name}</option>
                                ))}
                            </select>
                            <p className="text-xs text-gray-500 mt-1">
                                (If empty, add Fee Types in backend or request feature)
                            </p>
                        </div>

                        <div>
                            <label className="block text-sm mb-1">Frequency</label>
                            <select
                                className="input w-full"
                                value={form.frequency}
                                onChange={(e) => setForm({ ...form, frequency: e.target.value })}
                            >
                                {FREQUENCIES.map(f => <option key={f} value={f}>{f}</option>)}
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm mb-1">Amount (₹)</label>
                            <input
                                type="number"
                                className="input w-full"
                                value={form.amount}
                                onChange={(e) => setForm({ ...form, amount: e.target.value })}
                            />
                        </div>

                        <div className="flex justify-end gap-2 pt-2">
                            <button onClick={() => setShowModal(false)} className="text-gray-500 px-3">Cancel</button>
                            <button onClick={saveStructure} className="bg-blue-600 text-white px-4 py-2 rounded">Save</button>
                        </div>
                    </div>
                </div>
            )}

        </div>
    );
}
