"use client";

import { useState, useEffect } from "react";
import { financeApi, DailyCashSummary, FeeHeadSummary, ExpenseVoucherData } from "@/lib/financeApi";
import { useToast } from "@/components/ui/Toast";
import { Skeleton } from "@/components/ui/Skeleton";
import { downloadExcel } from "@/lib/fileUtils";

export default function DailyCashPage() {
    const { showToast } = useToast();

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

    useEffect(() => {
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

        fetchData();
    }, [selectedDate, showToast]);

    const handlePrint = () => {
        window.print();
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

    return (
        <div className="max-w-7xl mx-auto space-y-6">

            {/* Header controls (hidden when printing) */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 print:hidden">
                <h1 className="text-2xl font-bold text-gray-900">Daily Cash Dashboard</h1>
                <div className="flex items-center gap-4">
                    <input
                        type="date"
                        value={selectedDate}
                        onChange={(e) => setSelectedDate(e.target.value)}
                        disabled={loading}
                        className="px-3 py-2 border rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50"
                    />
                    <button
                        onClick={handleExport}
                        disabled={loading || isExporting}
                        className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 transition-colors flex items-center gap-2 font-medium"
                    >
                        {isExporting ? (
                            <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                        ) : (
                            <span>📊</span>
                        )}
                        Export Excel
                    </button>
                    <button
                        onClick={handlePrint}
                        disabled={loading || isExporting}
                        className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 disabled:opacity-50 transition-colors flex items-center gap-2"
                    >
                        <span>🖨️</span> Print View
                    </button>
                </div>
            </div>

            {/* Print Header (Visible only when printing) */}
            <div className="hidden print:block text-center border-b pb-4 mb-4">
                <h1 className="text-2xl font-bold">Daily Cash Dashboard</h1>
                <p className="text-gray-600 mt-1">Date: {new Date(selectedDate).toLocaleDateString()}</p>
            </div>

            {/* Summary Sections */}
            <div className="space-y-8">
                {/* Core Summary (Row 1) */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div className="bg-blue-50/50 rounded-xl border border-blue-100 p-6 flex items-center gap-4">
                        <div className="p-3 bg-blue-100 text-blue-600 rounded-lg text-2xl">💰</div>
                        <div>
                            <p className="text-xs font-bold uppercase tracking-wider text-blue-600/70">Total Revenue</p>
                            {loading ? (
                                <Skeleton className="h-8 w-24 mt-1" />
                            ) : (
                                <h3 className="text-2xl font-black text-blue-600">
                                    ₹{(summary?.totalFeeCollected ?? 0).toLocaleString("en-IN")}
                                </h3>
                            )}
                        </div>
                    </div>

                    <div className="bg-red-50/50 rounded-xl border border-red-100 p-6 flex items-center gap-4">
                        <div className="p-3 bg-red-100 text-red-600 rounded-lg text-2xl">📉</div>
                        <div>
                            <p className="text-xs font-bold uppercase tracking-wider text-red-600/70">Total Expense</p>
                            {loading ? (
                                <Skeleton className="h-8 w-24 mt-1" />
                            ) : (
                                <h3 className="text-2xl font-black text-red-600">
                                    ₹{(summary?.totalExpense ?? 0).toLocaleString("en-IN")}
                                </h3>
                            )}
                        </div>
                    </div>

                    <div className="bg-green-50/50 rounded-xl border border-green-100 p-6 flex items-center gap-4">
                        <div className="p-3 bg-green-100 text-green-600 rounded-lg text-2xl">⚖️</div>
                        <div>
                            <p className="text-xs font-bold uppercase tracking-wider text-green-600/70">Net</p>
                            {loading ? (
                                <Skeleton className="h-8 w-24 mt-1" />
                            ) : (
                                <h3 className={`text-2xl font-black ${(summary?.netAmount ?? 0) >= 0 ? "text-green-600" : "text-red-600"}`}>
                                    ₹{(summary?.netAmount ?? 0).toLocaleString("en-IN")}
                                </h3>
                            )}
                        </div>
                    </div>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    {/* Cash Section (Row 2) */}
                    <div className="bg-white rounded-2xl border shadow-sm overflow-hidden">
                        <div className="px-6 py-3 bg-amber-50 border-b border-amber-100 flex justify-between items-center">
                            <h4 className="text-xs font-black uppercase tracking-widest text-amber-700">Cash Flow</h4>
                            <span className="text-xl">💵</span>
                        </div>
                        <div className="p-6 grid grid-cols-3 gap-4">
                            <div>
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Revenue</p>
                                {loading ? <Skeleton className="h-6 w-16" /> : <p className="text-lg font-bold text-gray-800">₹{(summary?.cashRevenue ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                            <div>
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Expense</p>
                                {loading ? <Skeleton className="h-6 w-16" /> : <p className="text-lg font-bold text-red-600">₹{(summary?.cashExpense ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                            <div className="text-right">
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Net Cash</p>
                                {loading ? <Skeleton className="h-6 w-16 ml-auto" /> : <p className={`text-lg font-black ${(summary?.netCash ?? 0) >= 0 ? "text-green-600" : "text-red-600"}`}>₹{(summary?.netCash ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                        </div>
                    </div>

                    {/* Bank Section (Row 3 Equivalent Layout) */}
                    <div className="bg-white rounded-2xl border shadow-sm overflow-hidden">
                        <div className="px-6 py-3 bg-purple-50 border-b border-purple-100 flex justify-between items-center">
                            <h4 className="text-xs font-black uppercase tracking-widest text-purple-700">Bank Flow</h4>
                            <span className="text-xl">🏦</span>
                        </div>
                        <div className="p-6 grid grid-cols-3 gap-4">
                            <div>
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Revenue</p>
                                {loading ? <Skeleton className="h-6 w-16" /> : <p className="text-lg font-bold text-gray-800">₹{(summary?.bankRevenue ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                            <div>
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Expense</p>
                                {loading ? <Skeleton className="h-6 w-16" /> : <p className="text-lg font-bold text-red-600">₹{(summary?.bankExpense ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                            <div className="text-right">
                                <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Net Bank</p>
                                {loading ? <Skeleton className="h-6 w-16 ml-auto" /> : <p className={`text-lg font-black ${(summary?.netBank ?? 0) >= 0 ? "text-green-600" : "text-red-600"}`}>₹{(summary?.netBank ?? 0).toLocaleString("en-IN")}</p>}
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

                {/* Section A: Head-wise Collection */}
                <div className="bg-white rounded-xl shadow-sm border overflow-hidden flex flex-col">
                    <div className="px-6 py-4 border-b bg-gray-50">
                        <h2 className="text-lg font-semibold text-gray-900">Head-wise Collection</h2>
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
                <div className="bg-white rounded-xl shadow-sm border overflow-hidden flex flex-col">
                    <div className="px-6 py-4 border-b bg-gray-50">
                        <h2 className="text-lg font-semibold text-gray-900">Expense Breakdown</h2>
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
