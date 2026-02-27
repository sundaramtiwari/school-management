"use client";

import { useState, useEffect, useCallback } from "react";
import { financeApi, ExpenseHeadData } from "@/lib/financeApi";
import { useToast } from "@/components/ui/Toast";
import { Skeleton } from "@/components/ui/Skeleton";
import { useAuth } from "@/context/AuthContext";

export default function ExpenseHeadsPage() {
    const { user } = useAuth();
    const { showToast } = useToast();
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [heads, setHeads] = useState<ExpenseHeadData[]>([]);
    const [newName, setNewName] = useState("");
    const [newDescription, setNewDescription] = useState("");

    // Role protection
    const allowedRoles = ["SCHOOL_ADMIN", "ACCOUNTANT", "SUPER_ADMIN"];
    const userRole = user?.role?.toUpperCase();
    const isAuthorized = userRole && allowedRoles.includes(userRole);

    const fetchHeads = useCallback(async () => {
        setLoading(true);
        try {
            const data = await financeApi.getExpenseHeads();
            setHeads(data);
        } catch (error: any) {
            console.error("Error fetching expense heads:", error);
            showToast(error.message || "Failed to fetch expense heads", "error");
        } finally {
            setLoading(false);
        }
    }, [showToast]);

    useEffect(() => {
        if (isAuthorized) {
            fetchHeads();
        }
    }, [fetchHeads, isAuthorized]);

    const handleCreate = async (e: React.FormEvent) => {
        e.preventDefault();
        const trimmedName = newName.trim();
        if (!trimmedName) {
            showToast("Name is required", "error");
            return;
        }

        setSubmitting(true);
        try {
            await financeApi.createExpenseHead({
                name: trimmedName,
                description: newDescription.trim() || undefined
            });
            showToast("Expense head created successfully", "success");
            setNewName("");
            setNewDescription("");
            fetchHeads();
        } catch (error: any) {
            showToast(error.message || "Failed to create expense head", "error");
        } finally {
            setSubmitting(false);
        }
    };

    const handleToggleActive = async (id: number) => {
        try {
            await financeApi.toggleExpenseHeadActive(id);
            setHeads(prev => prev.map(head =>
                head.id === id ? { ...head, active: !head.active } : head
            ));
            showToast("Status updated successfully", "success");
        } catch (error: any) {
            showToast(error.message || "Failed to update status", "error");
        }
    };

    if (!isAuthorized) {
        return (
            <div className="flex flex-col items-center justify-center min-h-[60vh] space-y-4">
                <div className="text-6xl">🚫</div>
                <h1 className="text-2xl font-bold text-gray-900">Access Denied</h1>
                <p className="text-gray-500">You do not have permission to access this page.</p>
            </div>
        );
    }

    return (
        <div className="max-w-7xl mx-auto space-y-8">
            {/* Header Section */}
            <div>
                <h1 className="text-2xl font-bold text-gray-900">Expense Heads</h1>
                <p className="text-gray-500 text-sm">Manage expense categories used in vouchers</p>
            </div>

            {/* Create Head Card */}
            <div className="bg-white rounded-xl shadow-sm border p-6">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Create New Head</h2>
                <form onSubmit={handleCreate} className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
                    <div className="space-y-1">
                        <label className="text-sm font-medium text-gray-700">Name <span className="text-red-500">*</span></label>
                        <input
                            type="text"
                            required
                            placeholder="e.g. Electricity Bill"
                            value={newName}
                            onChange={(e) => setNewName(e.target.value)}
                            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 h-[42px]"
                            disabled={submitting}
                        />
                    </div>
                    <div className="space-y-1">
                        <label className="text-sm font-medium text-gray-700">Description (Optional)</label>
                        <input
                            type="text"
                            placeholder="Add some details..."
                            value={newDescription}
                            onChange={(e) => setNewDescription(e.target.value)}
                            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 h-[42px]"
                            disabled={submitting}
                        />
                    </div>
                    <div>
                        <button
                            type="submit"
                            disabled={submitting}
                            className="w-full md:w-auto px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium disabled:opacity-50 h-[42px]"
                        >
                            {submitting ? "Creating..." : "Create Head"}
                        </button>
                    </div>
                </form>
            </div>

            {/* Heads Table */}
            <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="border-b bg-gray-50 text-sm">
                                <th className="px-6 py-4 font-medium text-gray-500">Name</th>
                                <th className="px-6 py-4 font-medium text-gray-500">Description</th>
                                <th className="px-6 py-4 font-medium text-gray-500">Status</th>
                                <th className="px-6 py-4 font-medium text-gray-500 text-right">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y text-sm text-gray-700">
                            {loading ? (
                                Array.from({ length: 5 }).map((_, i) => (
                                    <tr key={i}>
                                        <td colSpan={4} className="px-6 py-4">
                                            <Skeleton className="h-6 w-full" />
                                        </td>
                                    </tr>
                                ))
                            ) : heads.length === 0 ? (
                                <tr>
                                    <td colSpan={4} className="px-6 py-12 text-center text-gray-500 italic">
                                        No expense heads created yet.
                                    </td>
                                </tr>
                            ) : (
                                heads.map((head) => (
                                    <tr key={head.id} className="hover:bg-gray-50 transition-colors">
                                        <td className="px-6 py-4 font-medium text-gray-900">
                                            {head.name}
                                        </td>
                                        <td className="px-6 py-4 text-gray-500">
                                            {head.description || <span className="text-gray-300">No description</span>}
                                        </td>
                                        <td className="px-6 py-4">
                                            <span className={`px-2 py-1 rounded text-[10px] font-black uppercase border ${head.active ? 'bg-green-50 text-green-700 border-green-100' : 'bg-gray-50 text-gray-700 border-gray-100'
                                                }`}>
                                                {head.active ? 'Active' : 'Inactive'}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 text-right">
                                            <button
                                                onClick={() => handleToggleActive(head.id)}
                                                className={`text-xs font-semibold px-3 py-1 rounded-lg border transition-all ${head.active
                                                        ? 'text-red-600 border-red-200 hover:bg-red-50'
                                                        : 'text-green-600 border-green-200 hover:bg-green-50'
                                                    }`}
                                            >
                                                {head.active ? 'Deactivate' : 'Activate'}
                                            </button>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
