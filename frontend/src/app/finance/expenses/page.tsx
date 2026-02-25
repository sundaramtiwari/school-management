"use client";

import { useState, useEffect, useCallback } from "react";
import { financeApi, ExpenseVoucherData } from "@/lib/financeApi";
import { useToast } from "@/components/ui/Toast";
import { Skeleton } from "@/components/ui/Skeleton";
import CreateExpenseModal from "./CreateExpenseModal";

export default function ExpensesPage() {
    const { showToast } = useToast();

    // Default to today
    const [selectedDate, setSelectedDate] = useState<string>(
        new Date().toISOString().split("T")[0]
    );

    const [loading, setLoading] = useState(true);
    const [expenses, setExpenses] = useState<ExpenseVoucherData[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);

    const fetchExpenses = useCallback(async () => {
        setLoading(true);
        try {
            const data = await financeApi.getExpensesByDate(selectedDate);
            setExpenses(data);
        } catch (error: any) {
            console.error("Error fetching expenses:", error);
            showToast(error.message || "Failed to fetch expenses", "error");
        } finally {
            setLoading(false);
        }
    }, [selectedDate, showToast]);

    useEffect(() => {
        fetchExpenses();
    }, [fetchExpenses]);

    return (
        <div className="max-w-7xl mx-auto space-y-6">

            {/* Header controls */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">Expenses</h1>
                    <p className="text-gray-500 text-sm">Manage and track school expenditures</p>
                </div>
                <div className="flex items-center gap-4">
                    <input
                        type="date"
                        value={selectedDate}
                        onChange={(e) => setSelectedDate(e.target.value)}
                        disabled={loading}
                        className="px-3 py-2 border rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50"
                    />
                    <button
                        onClick={() => setIsModalOpen(true)}
                        className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 font-medium"
                    >
                        <span>➕</span> Create Voucher
                    </button>
                </div>
            </div>

            {/* Main Table */}
            <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="border-b bg-gray-50 text-sm">
                                <th className="px-6 py-4 font-medium text-gray-500">Voucher No</th>
                                <th className="px-6 py-4 font-medium text-gray-500">Date</th>
                                <th className="px-6 py-4 font-medium text-gray-500">Expense Head</th>
                                <th className="px-6 py-4 font-medium text-gray-500 text-right">Amount</th>
                                <th className="px-6 py-4 font-medium text-gray-500">Payment Mode</th>
                                <th className="px-6 py-4 font-medium text-gray-500">Created By</th>
                                <th className="px-6 py-4 font-medium text-gray-500">Status</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y text-sm text-gray-700">
                            {loading ? (
                                Array.from({ length: 5 }).map((_, i) => (
                                    <tr key={i}>
                                        <td colSpan={7} className="px-6 py-4">
                                            <Skeleton className="h-6 w-full" />
                                        </td>
                                    </tr>
                                ))
                            ) : expenses.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="px-6 py-12 text-center text-gray-500 italic">
                                        No expense vouchers found for this date.
                                    </td>
                                </tr>
                            ) : (
                                expenses.map((expense) => (
                                    <tr key={expense.id} className="hover:bg-gray-50 transition-colors">
                                        <td className="px-6 py-4 font-mono text-xs font-bold text-gray-900">
                                            {expense.voucherNumber}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            {new Date(expense.expenseDate).toLocaleDateString()}
                                        </td>
                                        <td className="px-6 py-4">
                                            <div className="font-medium text-gray-900">{expense.expenseHeadName ?? 'Unknown Head'}</div>
                                            {expense.description && (
                                                <div className="text-xs text-gray-500 truncate max-w-[200px]" title={expense.description}>
                                                    {expense.description}
                                                </div>
                                            )}
                                        </td>
                                        <td className="px-6 py-4 text-right font-bold text-red-600 whitespace-nowrap">
                                            ₹{(expense.amount ?? 0).toLocaleString()}
                                        </td>
                                        <td className="px-6 py-4">
                                            <span className={`px-2 py-0.5 rounded text-[10px] font-black uppercase border ${expense.paymentMode === 'CASH' ? 'bg-amber-50 text-amber-700 border-amber-100' :
                                                expense.paymentMode === 'UPI' ? 'bg-blue-50 text-blue-700 border-blue-100' :
                                                    'bg-purple-50 text-purple-700 border-purple-100'
                                                }`}>
                                                {expense.paymentMode}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 text-gray-500">
                                            {expense.createdBy}
                                        </td>
                                        <td className="px-6 py-4">
                                            <span className={`px-2 py-0.5 rounded text-[10px] font-black uppercase border ${expense.active ? 'bg-green-50 text-green-700 border-green-100' : 'bg-red-50 text-red-700 border-red-100'
                                                }`}>
                                                {expense.active ? 'Active' : 'Cancelled'}
                                            </span>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                        {!loading && expenses.length > 0 && (
                            <tfoot>
                                <tr className="bg-gray-50 border-t font-bold">
                                    <td colSpan={3} className="px-6 py-4 text-right text-gray-900">Total Expenses for {new Date(selectedDate).toLocaleDateString()}</td>
                                    <td className="px-6 py-4 text-right text-red-700 text-lg">
                                        ₹{expenses.reduce((sum, e) => sum + (e.amount ?? 0), 0).toLocaleString()}
                                    </td>
                                    <td colSpan={3}></td>
                                </tr>
                            </tfoot>
                        )}
                    </table>
                </div>
            </div>

            {/* Modal */}
            <CreateExpenseModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onSuccess={fetchExpenses}
            />
        </div>
    );
}
