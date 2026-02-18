"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/components/ui/Toast";
import Modal from "@/components/ui/Modal";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { useSession } from "@/context/SessionContext";

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
};

const FREQUENCIES = ["ONE_TIME", "MONTHLY", "ANNUALLY"];

export default function FeeStructuresPage() {
    const { user } = useAuth();
    const { showToast } = useToast();
    const { currentSession } = useSession();
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [selectedClass, setSelectedClass] = useState<number | "">("");
    const [structures, setStructures] = useState<FeeStructure[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadingClasses, setLoadingClasses] = useState(true);
    const [isSaving, setIsSaving] = useState(false);

    const [showModal, setShowModal] = useState(false);
    const [showHeadModal, setShowHeadModal] = useState(false);
    const [feeTypes, setFeeTypes] = useState<any[]>([]);
    const [newHeadName, setNewHeadName] = useState("");
    const [isSavingHead, setIsSavingHead] = useState(false);

    const [form, setForm] = useState({
        feeTypeId: "",
        amount: "",
        frequency: "ONE_TIME",
    });

    useEffect(() => {
        loadClasses();
        loadFeeTypes();
    }, [currentSession]);

    async function loadClasses() {
        try {
            setLoadingClasses(true);
            const res = await api.get("/api/classes/mine?size=100");
            setClasses(res.data.content || []);
        } catch (e: any) {
            const msg = e.response?.data?.message || e.message;
            showToast("Failed to load classes: " + msg, "error");
        } finally {
            setLoadingClasses(false);
        }
    }

    async function loadFeeTypes() {
        try {
            const res = await api.get("/api/fees/types");
            setFeeTypes(res.data || []);
        } catch {
            // Optional fallback
        }
    }

    async function saveFeeHead() {
        if (!newHeadName.trim()) {
            showToast("Fee head name is required", "warning");
            return;
        }

        try {
            setIsSavingHead(true);
            await api.post("/api/fees/types", {
                name: newHeadName.trim(),
                description: newHeadName.trim()
            });
            showToast("Fee head added!", "success");
            setNewHeadName("");
            loadFeeTypes();
        } catch (e: any) {
            showToast("Failed to add head: " + (e.response?.data?.message || e.message), "error");
        } finally {
            setIsSavingHead(false);
        }
    }

    async function onClassChange(e: any) {
        const classId = e.target.value;
        setSelectedClass(classId);
        setStructures([]);
        if (classId) {
            loadStructures(classId);
        }
    }

    async function loadStructures(classId: number) {
        if (!currentSession) {
            return;
        }
        try {
            setLoading(true);
            const res = await api.get(`/api/fees/structures/by-class/${classId}?sessionId=${currentSession.id}`);
            setStructures(res.data || []);
        } catch (e: any) {
            const msg = e.response?.data?.message || e.message;
            showToast("Failed to load configurations: " + msg, "error");
        } finally {
            setLoading(false);
        }
    }

    async function saveStructure() {
        if (!selectedClass || !form.feeTypeId || !form.amount || !currentSession) {
            showToast(currentSession ? "All fields required" : "No active session", "warning");
            return;
        }

        try {
            setIsSaving(true);
            await api.post("/api/fees/structures", {
                classId: selectedClass,
                sessionId: currentSession.id,
                feeTypeId: form.feeTypeId,
                amount: Number(form.amount),
                frequency: form.frequency
            });
            showToast("Fee configuration saved!", "success");
            setShowModal(false);
            loadStructures(Number(selectedClass));
        } catch (e: any) {
            showToast("Save failed: " + (e.response?.data?.message || e.message), "error");
        } finally {
            setIsSaving(false);
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center text-wrap">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Fee Structures</h1>
                    <p className="text-gray-500">Configure fee heads for <span className="text-blue-600 font-bold">{currentSession?.name || "current session"}</span>.</p>
                </div>
                <div className="flex gap-3">
                    <button
                        onClick={() => setShowHeadModal(true)}
                        className="bg-white text-gray-700 px-6 py-2.5 rounded-xl font-bold border border-gray-200 shadow-sm hover:bg-gray-50 transition-all flex items-center gap-2"
                    >
                        <span className="text-xl">⚙️</span> Manage Heads
                    </button>
                    {/* Expand FE gating to match backend permissions */}
                    {["SCHOOL_ADMIN", "SUPER_ADMIN"].includes(user?.role?.toUpperCase() ?? "") && (
                        <button
                            disabled={!selectedClass}
                            onClick={() => setShowModal(true)}
                            className="bg-blue-600 text-white px-6 py-2.5 rounded-xl font-bold shadow-lg hover:bg-blue-700 transition-all flex items-center gap-2 disabled:bg-gray-400"
                        >
                            <span className="text-xl">+</span> Add Fee Configuration
                        </button>
                    )}
                </div>
            </div>

            <div className="bg-white p-6 border rounded-2xl shadow-sm flex flex-wrap gap-4 items-end">
                <div className="flex-1 min-w-[300px]">
                    <label className="block text-xs font-bold uppercase text-gray-400 mb-2 ml-1">Academic Target</label>
                    <select
                        className="input-ref"
                        value={selectedClass}
                        onChange={onClassChange}
                        disabled={loadingClasses}
                    >
                        <option value="">Select Academic Class</option>
                        {classes.map(c => (
                            <option key={c.id} value={c.id}>{c.name} {c.section}</option>
                        ))}
                    </select>
                </div>
            </div>

            {loading ? (
                <div className="bg-white p-8 rounded-2xl border">
                    <TableSkeleton rows={8} cols={4} />
                </div>
            ) : selectedClass ? (
                <div className="bg-white border rounded-2xl shadow-sm overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-gray-50 text-gray-600 font-bold border-b">
                            <tr>
                                <th className="p-4 text-left">Fee Category</th>
                                <th className="p-4 text-center">Frequency</th>
                                <th className="p-4 text-right">Amount Value</th>
                                <th className="p-4 text-center w-32">Status Flag</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {structures.map(fs => (
                                <tr key={fs.id} className="hover:bg-gray-50/50 transition-colors">
                                    <td className="p-4 font-bold text-gray-800">{fs.feeTypeName}</td>
                                    <td className="p-4 text-center">
                                        <span className="px-3 py-1 bg-blue-50 text-blue-600 rounded-lg text-[10px] font-black uppercase border border-blue-100">
                                            {fs.frequency.replace('_', ' ')}
                                        </span>
                                    </td>
                                    <td className="p-4 text-right font-black text-gray-900">
                                        ₹ {fs.amount.toLocaleString("en-IN")}
                                    </td>
                                    <td className="p-4 text-center">
                                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${fs.active ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-400"
                                            }`}>
                                            {fs.active ? "Active" : "Disabled"}
                                        </span>
                                    </td>
                                </tr>
                            ))}
                            {structures.length === 0 && (
                                <tr>
                                    <td colSpan={4} className="p-20 text-center text-gray-400 italic bg-gray-50/30">
                                        No fee heads configured for this class year.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            ) : (
                <div className="p-20 text-center bg-gray-50 rounded-2xl border border-dashed border-gray-300 text-gray-400 italic">
                    Configure institutional fees by selecting a primary class above.
                </div>
            )}

            <Modal
                isOpen={showModal}
                onClose={() => setShowModal(false)}
                title="Append Fee Configuration"
                maxWidth="max-w-md"
                footer={
                    <div className="flex gap-2">
                        <button
                            onClick={() => setShowModal(false)}
                            className="px-6 py-2 rounded-xl border font-medium text-gray-600 hover:bg-gray-50 transition-all"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={saveStructure}
                            disabled={isSaving}
                            className="px-8 py-2 rounded-xl bg-blue-600 text-white font-bold shadow-lg hover:bg-blue-700 disabled:bg-gray-400 transition-all"
                        >
                            {isSaving ? "Saving..." : "Commit Structure"}
                        </button>
                    </div>
                }
            >
                <div className="space-y-5">
                    <div>
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Fee Type Category *</label>
                        <select
                            className="input-ref font-bold"
                            value={form.feeTypeId}
                            onChange={(e) => setForm({ ...form, feeTypeId: e.target.value })}
                        >
                            <option value="">Select Ledger Head</option>
                            {feeTypes.map(t => (
                                <option key={t.id} value={t.id}>{t.name}</option>
                            ))}
                        </select>
                        {feeTypes.length === 0 && (
                            <p className="text-[10px] text-orange-600 mt-1 font-bold italic animate-pulse">
                                UI NOTE: No fee types detected. Please seed the database first.
                            </p>
                        )}
                    </div>

                    <div>
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Recurrence Frequency *</label>
                        <select
                            className="input-ref"
                            value={form.frequency}
                            onChange={(e) => setForm({ ...form, frequency: e.target.value })}
                        >
                            {FREQUENCIES.map(f => <option key={f} value={f}>{f.replace('_', ' ')}</option>)}
                        </select>
                    </div>

                    <div>
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Currency Amount (INR) *</label>
                        <div className="relative">
                            <span className="absolute left-4 top-1/2 -translate-y-1/2 font-bold text-gray-400">₹</span>
                            <input
                                type="number"
                                className="input-ref pl-10 font-bold text-lg"
                                value={form.amount}
                                onChange={(e) => setForm({ ...form, amount: e.target.value })}
                                placeholder="0.00"
                            />
                        </div>
                    </div>
                </div>
            </Modal>

            {/* Manage Fee Heads Modal */}
            <Modal
                isOpen={showHeadModal}
                onClose={() => setShowHeadModal(false)}
                title="Manage Institutional Fee Heads"
                maxWidth="max-w-md"
                footer={
                    <button
                        onClick={() => setShowHeadModal(false)}
                        className="px-6 py-2 rounded-xl bg-gray-900 text-white font-bold shadow-lg"
                    >
                        Done
                    </button>
                }
            >
                <div className="space-y-6">
                    <div>
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Add New Ledger Head</label>
                        <div className="flex gap-2">
                            <input
                                className="input-ref flex-1 font-bold"
                                placeholder="e.g., Library Fee, Sports Fund"
                                value={newHeadName}
                                onChange={(e) => setNewHeadName(e.target.value)}
                            />
                            <button
                                onClick={saveFeeHead}
                                disabled={isSavingHead}
                                className="bg-blue-600 text-white px-4 rounded-xl font-bold hover:bg-blue-700 disabled:bg-gray-400"
                            >
                                {isSavingHead ? "..." : "Add"}
                            </button>
                        </div>
                    </div>

                    <div className="border-t pt-4">
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-3 ml-1">Existing Heads</label>
                        <div className="space-y-2 max-h-[200px] overflow-y-auto pr-2">
                            {feeTypes.map(t => (
                                <div key={t.id} className="flex justify-between items-center p-3 bg-gray-50 rounded-xl border border-gray-100">
                                    <span className="font-bold text-gray-700">{t.name}</span>
                                    <span className="text-[10px] bg-gray-200 text-gray-500 px-2 py-0.5 rounded font-black uppercase">Active</span>
                                </div>
                            ))}
                            {feeTypes.length === 0 && (
                                <p className="text-center text-gray-400 py-4 italic">No ledger heads defined.</p>
                            )}
                        </div>
                    </div>
                </div>
            </Modal>
        </div>
    );
}
