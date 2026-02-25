"use client";

import { useState, useEffect } from "react";
import Modal from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { financeApi, ExpenseHeadData, ExpenseVoucherRequest } from "@/lib/financeApi";

interface CreateExpenseModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

export default function CreateExpenseModal({ isOpen, onClose, onSuccess }: CreateExpenseModalProps) {
    const { showToast } = useToast();
    const [loading, setLoading] = useState(false);
    const [heads, setHeads] = useState<ExpenseHeadData[]>([]);

    const [formData, setFormData] = useState<ExpenseVoucherRequest>({
        expenseDate: new Date().toISOString().split("T")[0],
        expenseHeadId: 0,
        amount: 0,
        paymentMode: "CASH",
        description: "",
        referenceNumber: ""
    });

    useEffect(() => {
        if (isOpen) {
            const fetchHeads = async () => {
                try {
                    const data = await financeApi.getExpenseHeads();
                    setHeads(data.filter(h => h.active));
                } catch (error: any) {
                    showToast(error.message || "Failed to fetch expense heads", "error");
                }
            };
            fetchHeads();
        }
    }, [isOpen, showToast]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (formData.expenseHeadId === 0) {
            showToast("Please select an expense head", "error");
            return;
        }

        if (formData.amount <= 0) {
            showToast("Amount must be greater than 0", "error");
            return;
        }

        setLoading(true);
        try {
            await financeApi.createExpense(formData);
            showToast("Expense voucher created successfully", "success");
            onSuccess();
            onClose();
            // Reset form
            setFormData({
                expenseDate: new Date().toISOString().split("T")[0],
                expenseHeadId: 0,
                amount: 0,
                paymentMode: "CASH",
                description: "",
                referenceNumber: ""
            });
        } catch (error: any) {
            showToast(error.message || "Failed to create expense voucher", "error");
        } finally {
            setLoading(true); // Disable while closing? No, set false.
            setLoading(false);
        }
    };

    const footer = (
        <>
            <button
                type="button"
                onClick={onClose}
                disabled={loading}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
            >
                Cancel
            </button>
            <button
                type="submit"
                form="create-expense-form"
                disabled={loading}
                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
            >
                {loading ? "Saving..." : "Create Voucher"}
            </button>
        </>
    );

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title="Create Expense Voucher"
            footer={footer}
        >
            <form id="create-expense-form" onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-1">
                        <label className="text-sm font-medium text-gray-700">Date</label>
                        <input
                            type="date"
                            required
                            value={formData.expenseDate}
                            onChange={(e) => setFormData({ ...formData, expenseDate: e.target.value })}
                            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                        />
                    </div>
                    <div className="space-y-1">
                        <label className="text-sm font-medium text-gray-700">Expense Head</label>
                        <select
                            required
                            value={formData.expenseHeadId}
                            onChange={(e) => setFormData({ ...formData, expenseHeadId: Number(e.target.value) })}
                            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                        >
                            <option value={0}>Select Head</option>
                            {heads.map(head => (
                                <option key={head.id} value={head.id}>{head.name}</option>
                            ))}
                        </select>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-1">
                        <label className="text-sm font-medium text-gray-700">Amount</label>
                        <input
                            type="number"
                            required
                            min="0.01"
                            step="0.01"
                            value={formData.amount}
                            onChange={(e) => setFormData({ ...formData, amount: Number(e.target.value) })}
                            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                        />
                    </div>
                    <div className="space-y-1">
                        <label className="text-sm font-medium text-gray-700">Payment Mode</label>
                        <select
                            required
                            value={formData.paymentMode}
                            onChange={(e) => setFormData({ ...formData, paymentMode: e.target.value as any })}
                            className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                        >
                            <option value="CASH">CASH</option>
                            <option value="BANK">BANK</option>
                            <option value="UPI">UPI</option>
                        </select>
                    </div>
                </div>

                <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-700">Reference Number (Optional)</label>
                    <input
                        type="text"
                        placeholder="Cheque No, Transaction ID, etc."
                        value={formData.referenceNumber}
                        onChange={(e) => setFormData({ ...formData, referenceNumber: e.target.value })}
                        className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    />
                </div>

                <div className="space-y-1">
                    <label className="text-sm font-medium text-gray-700">Description (Optional)</label>
                    <textarea
                        rows={3}
                        value={formData.description}
                        onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                        className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                        placeholder="Add some details about the expense..."
                    />
                </div>
            </form>
        </Modal>
    );
}
