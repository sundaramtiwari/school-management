"use client";

import { useState, useEffect } from "react";
import { financeApi, DailyCashSummary, FeeHeadSummary, ExpenseVoucherData, FinanceAccountTransferRequest } from "@/lib/financeApi";
import { useToast } from "@/components/ui/Toast";
import { Skeleton } from "@/components/ui/Skeleton";
import { downloadExcel } from "@/lib/fileUtils";
import Modal from "@/components/ui/Modal";
import { useAuth } from "@/context/AuthContext";

export default function DailyCashPage() {
    const { showToast } = useToast();
    const { user } = useAuth();
    const userRole = user?.role?.toUpperCase();

    // Default to today
    const [selectedDate, setSelectedDate] = useState<string>(
        new Date().toISOString().split("T")[0]
    );

    const [loading, setLoading] = useState(true);
    const [isExporting, setIsExporting] = useState(false);

    // Data state
    const [summary, setSummary] = useState<DailyCashSummary | null>(null);
    const [feeHeads, setFeeHeads] = useState<FeeHeadSummary[]>([]);
    const [expenses, setExpenses] = useState<ExpenseVoucherData[]>([]);

    // Transfer Modal state
    const [isTransferModalOpen, setIsTransferModalOpen] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [transferForm, setTransferForm] = useState<FinanceAccountTransferRequest>({
        transferDate: new Date().toISOString().split("T")[0],
        amount: 0,
        referenceNumber: "",
        remarks: ""
    });

    // Day Closing state
    const [isCloseDayModalOpen, setIsCloseDayModalOpen] = useState(false);
    const [isOverrideModalOpen, setIsOverrideModalOpen] = useState(false);
    const [isDayClosingSubmitting, setIsDayClosingSubmitting] = useState(false);
    const [isActionsOpen, setIsActionsOpen] = useState(false);

    const fetchData = async () => {
        setLoading(true);
        try {
            // Fetch daily cash summary
            const [summaryData, feeHeadsData, expensesData] = await Promise.all([
                financeApi.getDailyCashSummary(selectedDate),
                financeApi.getFeeHeadSummary(selectedDate),
                financeApi.getExpensesByDate(selectedDate)
            ]);

            setSummary(summaryData);
            setFeeHeads(feeHeadsData);
            setExpenses(expensesData);
        } catch (error: any) {
            console.error("Error fetching daily cash data:", error);
            showToast(error.message || "Failed to fetch daily cash data", "error");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, [selectedDate, showToast]);

    const handlePrint = () => {
        window.print();
    };

    const handleTransferSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (transferForm.amount <= 0) {
            showToast("Amount must be greater than 0", "error");
            return;
        }

        try {
            setIsSubmitting(true);
            await financeApi.createTransfer(transferForm);
            showToast("Cash deposit recorded successfully", "success");
            setIsTransferModalOpen(false);
            // Reset form
            setTransferForm({
                transferDate: new Date().toISOString().split("T")[0],
                amount: 0,
                referenceNumber: "",
                remarks: ""
            });
            // Refresh data
            fetchData();
        } catch (error: any) {
            console.error("Transfer failed:", error);
            showToast(error.message || "Failed to record transfer", "error");
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleExport = async () => {
        try {
            setIsExporting(true);
            const blob = await financeApi.exportDailyCash(selectedDate);
            downloadExcel(blob, `daily-cash-${selectedDate}.xlsx`);
            showToast("Export successful", "success");
        } catch (error: any) {
            console.error("Export failed:", error);
            showToast(error.message || "Export failed", "error");
        } finally {
            setIsExporting(false);
        }
    };

    const handleCloseDay = async () => {
        try {
            setIsDayClosingSubmitting(true);
            await financeApi.closeDay(selectedDate);
            showToast("Financial day closed successfully", "success");
            setIsCloseDayModalOpen(false);
            fetchData();
        } catch (error: any) {
            console.error("Day closing failed:", error);
            showToast(error.message || "Failed to close day", "error");
        } finally {
            setIsDayClosingSubmitting(false);
        }
    };

    const handleEnableOverride = async () => {
        try {
            setIsDayClosingSubmitting(true);
            await financeApi.enableOverride(selectedDate);
            showToast("Override enabled - you can now record transactions", "success");
            setIsOverrideModalOpen(false);
            fetchData();
        } catch (error: any) {
            console.error("Override failed:", error);
            showToast(error.message || "Failed to enable override", "error");
        } finally {
            setIsDayClosingSubmitting(false);
        }
    };

    return (
        <div className="max-w-7xl mx-auto space-y-6">

            {/* Header controls (hidden when printing) */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-baseline gap-4 print:hidden border-b pb-6">
                <div className="flex items-baseline gap-3">
                    <h1 className="text-xl font-bold text-gray-800 tracking-tight">Daily Cash Dashboard</h1>
                    {summary?.closed ? (
                        <span className="px-2 py-0.5 bg-red-50 text-red-600 text-[10px] font-bold uppercase tracking-wider rounded-full border border-red-100">
                            Closed
                        </span>
                    ) : (
                        <span className="px-2 py-0.5 bg-green-50 text-green-600 text-[10px] font-bold uppercase tracking-wider rounded-full border border-green-100">
                            Open
                        </span>
                    )}
                </div>

                <div className="flex items-center gap-3">
                    <input
                        type="date"
                        value={selectedDate}
                        onChange={(e) => setSelectedDate(e.target.value)}
                        disabled={loading}
                        className="px-3 py-1.5 text-sm border border-gray-200 rounded-lg shadow-sm focus:ring-1 focus:ring-gray-300 focus:border-gray-400 bg-white text-gray-700 font-medium outline-none transition-all disabled:opacity-50"
                    />

                    {/* Actions Dropdown */}
                    <div className="relative">
                        <button
                            onClick={() => setIsActionsOpen(!isActionsOpen)}
                            className="px-4 py-1.5 bg-white border border-gray-200 text-gray-700 text-sm font-semibold rounded-lg hover:bg-gray-50 flex items-center gap-2 shadow-sm transition-all active:scale-95"
                        >
                            Actions <span className={`text-[10px] transition-transform ${isActionsOpen ? 'rotate-180' : ''}`}>▼</span>
                        </button>

                        {isActionsOpen && (
                            <>
                                <div className="fixed inset-0 z-10" onClick={() => setIsActionsOpen(false)}></div>
                                <div className="absolute right-0 mt-2 w-56 bg-white border border-gray-100 rounded-xl shadow-xl z-20 py-2 animate-in fade-in zoom-in duration-75">
                                    <button
                                        onClick={() => { setIsTransferModalOpen(true); setIsActionsOpen(false); }}
                                        disabled={summary?.closed}
                                        className="w-full text-left px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-3 disabled:opacity-40 disabled:grayscale transition-colors"
                                    >
                                        <span className="text-base">📥</span> Transfer Cash to Bank
                                    </button>

                                    {!summary?.closed ? (
                                        (userRole === "SCHOOL_ADMIN" || userRole === "ACCOUNTANT" || userRole === "SUPER_ADMIN") && (
                                            <button
                                                onClick={() => { setIsCloseDayModalOpen(true); setIsActionsOpen(false); }}
                                                className="w-full text-left px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-3 transition-colors"
                                            >
                                                <span className="text-base text-gray-400">🔒</span> Close Day
                                            </button>
                                        )
                                    ) : (
                                        userRole === "SUPER_ADMIN" && (
                                            <button
                                                onClick={() => { setIsOverrideModalOpen(true); setIsActionsOpen(false); }}
                                                className="w-full text-left px-4 py-2.5 text-sm text-amber-700 hover:bg-amber-50 flex items-center gap-3 transition-colors"
                                            >
                                                <span className="text-base">🔓</span> Enable Override
                                            </button>
                                        )
                                    )}

                                    <div className="h-px bg-gray-100 my-1 mx-2"></div>

                                    <button
                                        onClick={() => { handleExport(); setIsActionsOpen(false); }}
                                        disabled={isExporting}
                                        className="w-full text-left px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-3 transition-colors"
                                    >
                                        <span className="text-base">{isExporting ? '⏳' : '📊'}</span> Export Excel
                                    </button>

                                    <button
                                        onClick={() => { handlePrint(); setIsActionsOpen(false); }}
                                        className="w-full text-left px-4 py-2.5 text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-3 transition-colors"
                                    >
                                        <span className="text-base">🖨️</span> Print View
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </div>

            {/* Print Header (Visible only when printing) */}
            <div className="hidden print:block text-center border-b pb-4 mb-4">
                <h1 className="text-2xl font-bold">Daily Cash Dashboard</h1>
                <p className="text-gray-600 mt-1">Date: {new Date(selectedDate).toLocaleDateString()}</p>
            </div>

            {/* Summary Sections */}
            <div className="space-y-12">
                {/* Core Summary (Row 1) */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 pt-4">
                    <div className="bg-white rounded-xl border border-gray-100 p-6 flex items-center gap-4 shadow-sm hover:border-blue-100 transition-colors">
                        <div className="p-3 bg-gray-50 text-gray-400 rounded-lg text-xl border border-gray-100">💰</div>
                        <div>
                            <p className="text-[10px] font-bold uppercase tracking-widest text-gray-400">Total Revenue</p>
                            {loading ? (
                                <Skeleton className="h-8 w-24 mt-1" />
                            ) : (
                                <h3 className="text-2xl font-bold text-gray-900 mt-1">
                                    ₹{(summary?.totalFeeCollected ?? 0).toLocaleString("en-IN")}
                                </h3>
                            )}
                        </div>
                    </div>

                    <div className="bg-white rounded-xl border border-gray-100 p-6 flex items-center gap-4 shadow-sm hover:border-red-100 transition-colors">
                        <div className="p-3 bg-gray-50 text-gray-400 rounded-lg text-xl border border-gray-100">📉</div>
                        <div>
                            <p className="text-[10px] font-bold uppercase tracking-widest text-gray-400">Total Expense</p>
                            {loading ? (
                                <Skeleton className="h-8 w-24 mt-1" />
                            ) : (
                                <h3 className="text-2xl font-bold text-gray-900 mt-1">
                                    ₹{(summary?.totalExpense ?? 0).toLocaleString("en-IN")}
                                </h3>
                            )}
                        </div>
                    </div>

                    <div className="bg-white rounded-xl border border-gray-100 p-6 flex items-center gap-4 shadow-sm hover:border-green-100 transition-colors">
                        <div className="p-3 bg-gray-50 text-gray-400 rounded-lg text-xl border border-gray-100">⚖️</div>
                        <div>
                            <p className="text-[10px] font-bold uppercase tracking-widest text-gray-400">Net Balance</p>
                            {loading ? (
                                <Skeleton className="h-8 w-24 mt-1" />
                            ) : (
                                <h3 className={`text-2xl font-bold mt-1 ${(summary?.netAmount ?? 0) >= 0 ? "text-green-600" : "text-red-600"}`}>
                                    ₹{(summary?.netAmount ?? 0).toLocaleString("en-IN")}
                                </h3>
                            )}
                        </div>
                    </div>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
                    {/* Cash Section (Row 2) */}
                    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
                        <div className="px-6 py-4 bg-gray-50/50 border-b border-gray-100 flex justify-between items-center">
                            <h4 className="text-xs font-bold uppercase tracking-widest text-gray-500">Cash Flow</h4>
                            <span className="text-lg grayscale opacity-50">💵</span>
                        </div>
                        <div className="p-6 grid grid-cols-4 gap-4">
                            <div>
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Revenue</p>
                                {loading ? <Skeleton className="h-6 w-16" /> : <p className="text-lg font-bold text-gray-800">₹{(summary?.cashRevenue ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                            <div>
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Expense</p>
                                {loading ? <Skeleton className="h-6 w-16" /> : <p className="text-lg font-bold text-red-600">₹{(summary?.cashExpense ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                            <div>
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Trans Out</p>
                                {loading ? <Skeleton className="h-6 w-16" /> : <p className="text-lg font-bold text-orange-500">₹{(summary?.transferOut ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                            <div className="text-right border-l pl-4">
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Net Cash</p>
                                {loading ? <Skeleton className="h-6 w-16 ml-auto" /> : <p className={`text-lg font-bold ${(summary?.netCash ?? 0) >= 0 ? "text-green-600" : "text-red-600"}`}>₹{(summary?.netCash ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                        </div>
                    </div>

                    {/* Bank Section (Row 3 Equivalent Layout) */}
                    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
                        <div className="px-6 py-4 bg-gray-50/50 border-b border-gray-100 flex justify-between items-center">
                            <h4 className="text-xs font-bold uppercase tracking-widest text-gray-500">Bank Flow</h4>
                            <span className="text-lg grayscale opacity-50">🏦</span>
                        </div>
                        <div className="p-6 grid grid-cols-4 gap-4">
                            <div>
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Revenue</p>
                                {loading ? <Skeleton className="h-6 w-16" /> : <p className="text-lg font-bold text-gray-800">₹{(summary?.bankRevenue ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                            <div>
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Expense</p>
                                {loading ? <Skeleton className="h-6 w-16" /> : <p className="text-lg font-bold text-red-600">₹{(summary?.bankExpense ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                            <div>
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Trans In</p>
                                {loading ? <Skeleton className="h-6 w-16" /> : <p className="text-lg font-bold text-indigo-500">₹{(summary?.transferIn ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                            <div className="text-right border-l pl-4">
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Net Bank</p>
                                {loading ? <Skeleton className="h-6 w-16 ml-auto" /> : <p className={`text-lg font-bold ${(summary?.netBank ?? 0) >= 0 ? "text-green-600" : "text-red-600"}`}>₹{(summary?.netBank ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-10 mt-12 pb-20">
                {/* Section A: Head-wise Collection */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden flex flex-col">
                    <div className="px-6 py-4 border-b bg-gray-50/30">
                        <h2 className="text-sm font-bold text-gray-700 uppercase tracking-widest">Head-wise Collection</h2>
                    </div>
                    <div className="p-0 flex-1 overflow-x-auto">
                        <table className="w-full text-left border-collapse">
                            <thead>
                                <tr className="border-b bg-gray-50 text-sm">
                                    <th className="px-6 py-3 font-medium text-gray-500">Fee Type</th>
                                    <th className="px-6 py-3 font-medium text-gray-500 text-right">Principal</th>
                                    <th className="px-6 py-3 font-medium text-gray-500 text-right">Late Fee</th>
                                    <th className="px-6 py-3 font-medium text-gray-500 text-right">Total</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y text-sm">
                                {loading ? (
                                    <tr>
                                        <td colSpan={4} className="p-4 space-y-2">
                                            <Skeleton className="h-4 w-full" />
                                            <Skeleton className="h-4 w-full" />
                                            <Skeleton className="h-4 w-full" />
                                        </td>
                                    </tr>
                                ) : feeHeads.length === 0 ? (
                                    <tr>
                                        <td colSpan={4} className="px-6 py-8 text-center text-gray-500 italic">
                                            No fee collections for this date.
                                        </td>
                                    </tr>
                                ) : (
                                    feeHeads.map((item, idx) => (
                                        <tr key={idx} className="hover:bg-gray-50">
                                            <td className="px-6 py-3 font-medium text-gray-900">{item.feeTypeName}</td>
                                            <td className="px-6 py-3 text-right">₹{(item.totalPrincipal ?? 0).toLocaleString()}</td>
                                            <td className="px-6 py-3 text-right">₹{(item.totalLateFee ?? 0).toLocaleString()}</td>
                                            <td className="px-6 py-3 text-right font-bold text-gray-900">₹{(item.totalCollected ?? 0).toLocaleString()}</td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                            {!loading && feeHeads.length > 0 && (
                                <tfoot>
                                    <tr className="border-t-2 border-gray-200 bg-gray-50 text-sm font-bold shadow-sm">
                                        <td className="px-6 py-3 text-gray-900">Total</td>
                                        <td className="px-6 py-3 text-right text-gray-900">
                                            ₹{feeHeads.reduce((sum, item) => sum + (item.totalPrincipal ?? 0), 0).toLocaleString()}
                                        </td>
                                        <td className="px-6 py-3 text-right text-gray-900">
                                            ₹{feeHeads.reduce((sum, item) => sum + (item.totalLateFee ?? 0), 0).toLocaleString()}
                                        </td>
                                        <td className="px-6 py-3 text-right text-green-700">
                                            ₹{feeHeads.reduce((sum, item) => sum + (item.totalCollected ?? 0), 0).toLocaleString()}
                                        </td>
                                    </tr>
                                </tfoot>
                            )}
                        </table>
                    </div>
                </div>

                {/* Section B: Expense Breakdown */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden flex flex-col">
                    <div className="px-6 py-4 border-b bg-gray-50/30">
                        <h2 className="text-sm font-bold text-gray-700 uppercase tracking-widest">Expense Breakdown</h2>
                    </div>
                    <div className="p-0 flex-1 overflow-x-auto">
                        <table className="w-full text-left border-collapse">
                            <thead>
                                <tr className="border-b bg-gray-50 text-sm">
                                    <th className="px-6 py-3 font-medium text-gray-500">Expense Head</th>
                                    <th className="px-6 py-3 font-medium text-gray-500 text-right">Amount</th>
                                    <th className="px-6 py-3 font-medium text-gray-500">Payment Mode</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y text-sm">
                                {loading ? (
                                    <tr>
                                        <td colSpan={3} className="p-4 space-y-2">
                                            <Skeleton className="h-4 w-full" />
                                            <Skeleton className="h-4 w-full" />
                                            <Skeleton className="h-4 w-full" />
                                        </td>
                                    </tr>
                                ) : expenses.length === 0 ? (
                                    <tr>
                                        <td colSpan={3} className="px-6 py-8 text-center text-gray-500 italic">
                                            No expenses recorded for this date.
                                        </td>
                                    </tr>
                                ) : (
                                    expenses.map((expense) => (
                                        <tr key={expense.id} className="hover:bg-gray-50">
                                            <td className="px-6 py-3">
                                                <div className="font-medium text-gray-900">
                                                    {expense.expenseHeadName ?? 'Unknown Head'}
                                                </div>
                                                {expense.description && (
                                                    <div className="text-xs text-gray-500 mt-1 truncate max-w-[200px]" title={expense.description}>
                                                        {expense.description}
                                                    </div>
                                                )}
                                            </td>
                                            <td className="px-6 py-3 text-right font-medium text-red-600">
                                                ₹{(expense.amount ?? 0).toLocaleString()}
                                            </td>
                                            <td className="px-6 py-3">
                                                <span className={`px-2 py-0.5 rounded text-xs font-medium ${expense.paymentMode === 'CASH' ? 'bg-amber-100 text-amber-800' :
                                                    expense.paymentMode === 'UPI' ? 'bg-blue-100 text-blue-800' :
                                                        'bg-purple-100 text-purple-800'
                                                    }`}>
                                                    {expense.paymentMode}
                                                </span>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                            {!loading && expenses.length > 0 && (
                                <tfoot>
                                    <tr className="border-t-2 border-gray-200 bg-gray-50 text-sm font-bold shadow-sm">
                                        <td className="px-6 py-3 text-gray-900 text-right">Total</td>
                                        <td className="px-6 py-3 text-right text-red-700">
                                            ₹{expenses.reduce((sum, item) => sum + (item.amount ?? 0), 0).toLocaleString()}
                                        </td>
                                        <td className="px-6 py-3"></td>
                                    </tr>
                                </tfoot>
                            )}
                        </table>
                    </div>
                </div>

            </div>
            {/* Record Cash Deposit Modal */}
            <Modal
                isOpen={isTransferModalOpen}
                onClose={() => !isSubmitting && setIsTransferModalOpen(false)}
                title="Record Cash Deposit (Cash ➔ Bank)"
                maxWidth="max-w-md"
            >
                <form onSubmit={handleTransferSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-bold text-gray-700 mb-1">Date</label>
                        <input
                            type="date"
                            required
                            value={transferForm.transferDate}
                            onChange={(e) => setTransferForm({ ...transferForm, transferDate: e.target.value })}
                            className="w-full px-4 py-2 border rounded-xl focus:ring-2 focus:ring-blue-500 bg-gray-50 font-medium"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-bold text-gray-700 mb-1">Amount (₹)</label>
                        <input
                            type="number"
                            required
                            min="0.01"
                            step="0.01"
                            placeholder="0.00"
                            value={transferForm.amount || ""}
                            onChange={(e) => setTransferForm({ ...transferForm, amount: parseFloat(e.target.value) })}
                            className="w-full px-4 py-2 border rounded-xl focus:ring-2 focus:ring-blue-500 font-bold text-lg"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-bold text-gray-700 mb-1">Reference Number (Optional)</label>
                        <input
                            type="text"
                            placeholder="Bank Txn ID / Receipt No"
                            value={transferForm.referenceNumber || ""}
                            onChange={(e) => setTransferForm({ ...transferForm, referenceNumber: e.target.value })}
                            className="w-full px-4 py-2 border rounded-xl focus:ring-2 focus:ring-blue-500"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-bold text-gray-700 mb-1">Remarks (Optional)</label>
                        <textarea
                            rows={2}
                            placeholder="Add any internal notes..."
                            value={transferForm.remarks || ""}
                            onChange={(e) => setTransferForm({ ...transferForm, remarks: e.target.value })}
                            className="w-full px-4 py-2 border rounded-xl focus:ring-2 focus:ring-blue-500 text-sm"
                        />
                    </div>

                    <div className="pt-2 flex flex-col gap-3">
                        <button
                            type="submit"
                            disabled={isSubmitting || loading}
                            className={`w-full py-3 rounded-xl font-bold transition-all shadow-md active:scale-[0.98] ${isSubmitting
                                ? "bg-gray-100 text-gray-400 cursor-not-allowed"
                                : "bg-blue-600 text-white hover:bg-blue-700"
                                }`}
                        >
                            {isSubmitting ? (
                                <span className="flex items-center justify-center gap-2">
                                    <span className="w-4 h-4 border-2 border-gray-300 border-t-gray-600 rounded-full animate-spin"></span>
                                    Recording...
                                </span>
                            ) : (
                                "Record Deposit"
                            )}
                        </button>
                        <button
                            type="button"
                            onClick={() => setIsTransferModalOpen(false)}
                            disabled={isSubmitting}
                            className="w-full py-2 text-sm font-medium text-gray-500 hover:text-gray-700 transition-colors"
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </Modal>

            {/* Close Day Modal */}
            <Modal
                isOpen={isCloseDayModalOpen}
                onClose={() => !isDayClosingSubmitting && setIsCloseDayModalOpen(false)}
                title="Close Financial Day"
                maxWidth="max-w-md"
            >
                <div className="space-y-4">
                    <div className="p-4 bg-red-50 border border-red-100 rounded-xl flex gap-3">
                        <span className="text-2xl">⚠️</span>
                        <div>
                            <p className="text-sm font-bold text-red-800">Final Action Warning</p>
                            <p className="text-sm text-red-700 mt-1">
                                This will <strong>lock all financial transactions</strong> for {new Date(selectedDate).toLocaleDateString()}.
                                You will not be able to add payments, expenses, or transfers for this date.
                            </p>
                        </div>
                    </div>

                    <div className="flex flex-col gap-3">
                        <button
                            onClick={handleCloseDay}
                            disabled={isDayClosingSubmitting}
                            className="w-full py-3 bg-red-600 text-white rounded-xl font-bold hover:bg-red-700 transition-all shadow-md active:scale-[0.98] disabled:opacity-50"
                        >
                            {isDayClosingSubmitting ? (
                                <span className="flex items-center justify-center gap-2">
                                    <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                                    Closing Day...
                                </span>
                            ) : (
                                "Confirm Close Day"
                            )}
                        </button>
                        <button
                            onClick={() => setIsCloseDayModalOpen(false)}
                            disabled={isDayClosingSubmitting}
                            className="w-full py-2 text-sm font-medium text-gray-500 hover:text-gray-700 transition-colors"
                        >
                            Cancel
                        </button>
                    </div>
                </div>
            </Modal>

            {/* Override Modal */}
            <Modal
                isOpen={isOverrideModalOpen}
                onClose={() => !isDayClosingSubmitting && setIsOverrideModalOpen(false)}
                title="Enable Override"
                maxWidth="max-w-md"
            >
                <div className="space-y-4">
                    <div className="p-4 bg-amber-50 border border-amber-100 rounded-xl flex gap-3">
                        <span className="text-2xl">⚡</span>
                        <div>
                            <p className="text-sm font-bold text-amber-800">Super Admin Override</p>
                            <p className="text-sm text-amber-700 mt-1">
                                This allows financial edits for this closed date.
                                Use with care for audit purposes.
                            </p>
                        </div>
                    </div>

                    <div className="flex flex-col gap-3">
                        <button
                            onClick={handleEnableOverride}
                            disabled={isDayClosingSubmitting}
                            className="w-full py-3 bg-amber-500 text-white rounded-xl font-bold hover:bg-amber-600 transition-all shadow-md active:scale-[0.98] disabled:opacity-50"
                        >
                            {isDayClosingSubmitting ? (
                                <span className="flex items-center justify-center gap-2">
                                    <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                                    Enabling...
                                </span>
                            ) : (
                                "Confirm Override"
                            )}
                        </button>
                        <button
                            onClick={() => setIsOverrideModalOpen(false)}
                            disabled={isDayClosingSubmitting}
                            className="w-full py-2 text-sm font-medium text-gray-500 hover:text-gray-700 transition-colors"
                        >
                            Cancel
                        </button>
                    </div>
                </div>
            </Modal>

            {/* 
            styles for printing layout properly
            */}
            <style jsx global>{`
                @media print {
                    body {
                        background-color: white !important;
                    }
                    .shadow-sm {
                        box-shadow: none !important;
                    }
                    .border {
                        border-color: #e5e7eb !important;
                    }
                    .bg-white {
                        background-color: transparent !important;
                    }
                    .bg-gray-50 {
                        background-color: #f9fafb !important;
                        -webkit-print-color-adjust: exact;
                        print-color-adjust: exact;
                    }
                }
            `}</style>
        </div>
    );
}
