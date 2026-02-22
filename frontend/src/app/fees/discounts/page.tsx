"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/components/ui/Toast";
import Modal from "@/components/ui/Modal";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { useRouter } from "next/navigation";

type DiscountDefinition = {
    id: number;
    name: string;
    type: string;
    amountValue: number;
    active: boolean;
};

export default function DiscountDefinitionsPage() {
    const { user } = useAuth();
    const { showToast } = useToast();
    const router = useRouter();

    const [discounts, setDiscounts] = useState<DiscountDefinition[]>([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    const [form, setForm] = useState({
        name: "",
        type: "FLAT",
        amountValue: "",
        active: true
    });

    const canManageFees = ["SCHOOL_ADMIN", "SUPER_ADMIN", "PLATFORM_ADMIN"].includes(user?.role?.toUpperCase() ?? "");

    const loadDiscounts = useCallback(async () => {
        try {
            setLoading(true);
            const res = await api.get("/api/fees/discount-definitions");
            setDiscounts(res.data || []);
        } catch (e: any) {
            showToast(e?.response?.data?.message || "Failed to load discount definitions", "error");
        } finally {
            setLoading(false);
        }
    }, [showToast]);

    useEffect(() => {
        loadDiscounts();
    }, [loadDiscounts]);

    async function handleSave() {
        if (!form.name.trim() || !form.amountValue) {
            showToast("Please fill all required fields", "warning");
            return;
        }

        const amount = Number(form.amountValue);
        if (amount <= 0) {
            showToast("Amount must be greater than zero", "warning");
            return;
        }

        if (form.type === "PERCENTAGE" && amount > 100) {
            showToast("Percentage cannot exceed 100", "warning");
            return;
        }

        try {
            setIsSaving(true);
            await api.post("/api/fees/discount-definitions", {
                name: form.name.trim(),
                type: form.type,
                amountValue: amount,
                active: form.active
            });
            showToast("Discount created", "success");
            setShowModal(false);
            setForm({ name: "", type: "FLAT", amountValue: "", active: true });
            loadDiscounts();
        } catch (e: any) {
            showToast(e?.response?.data?.message || "Failed to create discount", "error");
        } finally {
            setIsSaving(false);
        }
    }

    async function toggleActive(id: number, currentStatus: boolean) {
        if (!canManageFees) return;

        try {
            // Optimistic update
            setDiscounts(prev =>
                prev.map(d => d.id === id ? { ...d, active: !currentStatus } : d)
            );

            await api.patch(`/api/fees/discount-definitions/${id}/toggle`);
            showToast(`Discount ${!currentStatus ? 'activated' : 'deactivated'}`, "success");
        } catch (e: any) {
            // Revert on failure
            setDiscounts(prev =>
                prev.map(d => d.id === id ? { ...d, active: currentStatus } : d)
            );
            showToast(e?.response?.data?.message || "Failed to update status", "error");
        }
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center text-wrap">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Discount Policies</h1>
                    <p className="text-gray-500">Manage fee discounts and concessions.</p>
                </div>
                <div className="flex gap-3">
                    <button
                        onClick={() => router.back()}
                        className="bg-white text-gray-700 px-6 py-2.5 rounded-xl font-bold border border-gray-200 shadow-sm hover:bg-gray-50 transition-all"
                    >
                        Back
                    </button>
                    {canManageFees && (
                        <button
                            onClick={() => setShowModal(true)}
                            className="bg-blue-600 text-white px-6 py-2.5 rounded-xl font-bold shadow-lg hover:bg-blue-700 transition-all flex items-center gap-2"
                        >
                            <span className="text-xl">+</span> Create Discount
                        </button>
                    )}
                </div>
            </div>

            {loading ? (
                <div className="bg-white p-8 rounded-2xl border">
                    <TableSkeleton rows={5} cols={5} />
                </div>
            ) : (
                <div className="bg-white border rounded-2xl shadow-sm overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-gray-50 text-gray-600 font-bold border-b">
                            <tr>
                                <th className="p-4 text-left">Discount Name</th>
                                <th className="p-4 text-center">Type</th>
                                <th className="p-4 text-right">Value</th>
                                <th className="p-4 text-center">Status</th>
                                {canManageFees && <th className="p-4 text-center">Actions</th>}
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {discounts.map(d => (
                                <tr key={d.id} className="hover:bg-gray-50/50 transition-colors">
                                    <td className="p-4 font-bold text-gray-800">{d.name}</td>
                                    <td className="p-4 text-center">
                                        <span className="px-3 py-1 bg-blue-50 text-blue-600 rounded-lg text-[10px] font-black uppercase border border-blue-100">
                                            {d.type}
                                        </span>
                                    </td>
                                    <td className="p-4 text-right font-black text-gray-900">
                                        {d.type === "FLAT" ? `₹ ${d.amountValue.toLocaleString("en-IN")}` : `${d.amountValue}%`}
                                    </td>
                                    <td className="p-4 text-center">
                                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${d.active ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-400"
                                            }`}>
                                            {d.active ? "Active" : "Inactive"}
                                        </span>
                                    </td>
                                    {canManageFees && (
                                        <td className="p-4 text-center">
                                            <button
                                                onClick={() => toggleActive(d.id, d.active)}
                                                className={`px-4 py-1.5 rounded-lg text-xs font-bold transition-colors ${d.active
                                                        ? "bg-red-50 text-red-600 hover:bg-red-100"
                                                        : "bg-green-50 text-green-600 hover:bg-green-100"
                                                    }`}
                                            >
                                                {d.active ? "Deactivate" : "Activate"}
                                            </button>
                                        </td>
                                    )}
                                </tr>
                            ))}
                            {discounts.length === 0 && (
                                <tr>
                                    <td colSpan={canManageFees ? 5 : 4} className="p-20 text-center text-gray-400 italic bg-gray-50/30">
                                        No discount policies configured yet.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            )}

            <Modal
                isOpen={showModal}
                onClose={() => setShowModal(false)}
                title="Create Discount Policy"
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
                            onClick={handleSave}
                            disabled={isSaving}
                            className="px-8 py-2 rounded-xl bg-blue-600 text-white font-bold shadow-lg hover:bg-blue-700 disabled:bg-gray-400 transition-all"
                        >
                            {isSaving ? "Saving..." : "Create"}
                        </button>
                    </div>
                }
            >
                <div className="space-y-5">
                    <div>
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Discount Name *</label>
                        <input
                            className="input-ref w-full"
                            placeholder="e.g., Sibling Discount, Staff Concession"
                            value={form.name}
                            onChange={(e) => setForm({ ...form, name: e.target.value })}
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Type *</label>
                            <select
                                className="input-ref w-full font-bold"
                                value={form.type}
                                onChange={(e) => setForm({ ...form, type: e.target.value, amountValue: "" })}
                            >
                                <option value="FLAT">Flat Amount (₹)</option>
                                <option value="PERCENTAGE">Percentage (%)</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Value *</label>
                            <input
                                type="number"
                                className="input-ref w-full font-bold"
                                value={form.amountValue}
                                onChange={(e) => setForm({ ...form, amountValue: e.target.value })}
                                placeholder={form.type === "FLAT" ? "0.00" : "0-100"}
                            />
                        </div>
                    </div>

                    <div className="flex items-center gap-2 pt-2">
                        <input
                            type="checkbox"
                            id="active-toggle"
                            checked={form.active}
                            onChange={(e) => setForm({ ...form, active: e.target.checked })}
                            className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
                        />
                        <label htmlFor="active-toggle" className="text-sm font-bold text-gray-700">
                            Active immediately
                        </label>
                    </div>
                </div>
            </Modal>
        </div>
    );
}
