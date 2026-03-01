"use client";

import { useState, useEffect, useCallback } from "react";
import { financeApi, FinancialOverviewData } from "@/lib/financeApi";
import { useToast } from "@/components/ui/Toast";
import { Skeleton } from "@/components/ui/Skeleton";
import { downloadExcel } from "@/lib/fileUtils";

export default function RangePLOverview() {
    const { showToast } = useToast();

    // Default range: April 1 of current year (or previous if before April) to today
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth(); // 0-indexed
    const fiscalYearStart = currentMonth < 3 ? currentYear - 1 : currentYear;
    const defaultStart = `${fiscalYearStart}-04-01`;
    const defaultEnd = now.toISOString().split("T")[0];

    // Local state for UI inputs (doesn't trigger fetch)
    const [startDateInput, setStartDateInput] = useState<string>(defaultStart);
    const [endDateInput, setEndDateInput] = useState<string>(defaultEnd);

    // Effective state (used for fetching and labels)
    const [startDate, setStartDate] = useState<string>(defaultStart);
    const [endDate, setEndDate] = useState<string>(defaultEnd);

    const [loading, setLoading] = useState(false);
    const [isExporting, setIsExporting] = useState(false);
    const [data, setData] = useState<FinancialOverviewData | null>(null);
    const [error, setError] = useState<string | null>(null);

    const fetchData = useCallback(async (startToUse: string, endToUse: string) => {
        if (!startToUse || !endToUse) return;
        setLoading(true);
        setError(null);
        try {
            const result = await financeApi.getRangePL(startToUse, endToUse);
            setData(result);
            // Sync effective state only on success
            setStartDate(startToUse);
            setEndDate(endToUse);
        } catch (err: any) {
            console.error("Error fetching P&L:", err);
            setError(err.message || "Failed to fetch P&L data");
            showToast(err.message || "Failed to fetch P&L data", "error");
        } finally {
            setLoading(false);
        }
    }, [showToast]);

    // Initial load removed to enforce manual fetching

    const handleGo = () => {
        fetchData(startDateInput, endDateInput);
    };

    const handleExport = async () => {
        try {
            setIsExporting(true);
            const blob = await financeApi.exportRangePL(startDate, endDate);
            downloadExcel(blob, `pl-report-${startDate}-to-${endDate}.xlsx`);
            showToast("Export successful", "success");
        } catch (error: any) {
            console.error("Export failed:", error);
            showToast(error.message || "Export failed", "error");
        } finally {
            setIsExporting(false);
        }
    };

    const formatCurrency = (amount: number = 0) => {
        return `₹${amount.toLocaleString("en-IN")}`;
    };

    return (
        <div className="space-y-8">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b pb-6">
                <div>
                    <h2 className="text-xl font-bold text-gray-800 tracking-tight">Accounting View (P&L)</h2>
                    <p className="text-sm text-gray-500">Business performance metrics for selected duration</p>
                </div>
                <div className="flex flex-wrap items-center gap-3">
                    <div className="flex items-center gap-2 bg-white border border-gray-200 rounded-lg px-2 py-1 shadow-sm">
                        <span className="text-[10px] font-bold text-gray-400 uppercase">From</span>
                        <input
                            type="date"
                            value={startDateInput}
                            onChange={(e) => setStartDateInput(e.target.value)}
                            disabled={loading}
                            className="text-sm font-medium border-none focus:ring-0 outline-none p-0.5"
                        />
                        <span className="text-[10px] font-bold text-gray-400 uppercase ml-2">To</span>
                        <input
                            type="date"
                            value={endDateInput}
                            onChange={(e) => setEndDateInput(e.target.value)}
                            disabled={loading}
                            className="text-sm font-medium border-none focus:ring-0 outline-none p-0.5"
                        />
                    </div>

                    <button
                        onClick={handleGo}
                        disabled={loading}
                        className="px-6 py-2 bg-green-600 text-white text-sm rounded-lg hover:bg-green-700 disabled:opacity-50 transition-all active:scale-95 font-bold shadow-sm min-w-[80px] flex justify-center items-center"
                    >
                        {loading ? (
                            <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                        ) : (
                            "Go"
                        )}
                    </button>

                    <button
                        onClick={handleExport}
                        disabled={loading || isExporting}
                        className="px-4 py-2 bg-gray-100 text-gray-700 text-sm rounded-lg hover:bg-gray-200 disabled:opacity-50 transition-colors flex items-center gap-2 font-semibold shadow-sm border border-gray-200"
                    >
                        {isExporting ? (
                            <span className="w-4 h-4 border-2 border-gray-400/30 border-t-gray-600 rounded-full animate-spin"></span>
                        ) : (
                            <span>📊</span>
                        )}
                        Export
                    </button>
                </div>
            </div>

            {error ? (
                <div className="bg-red-50 border border-red-200 rounded-2xl p-8 text-center">
                    <div className="text-4xl mb-4">⚠️</div>
                    <h3 className="text-lg font-bold text-red-800 mb-2">Data Fetch Failed</h3>
                    <p className="text-red-600 mb-6">{error}</p>
                    <button
                        onClick={handleGo}
                        className="px-6 py-2 bg-red-600 text-white rounded-xl font-bold hover:bg-red-700 transition-colors"
                    >
                        Retry Fetch
                    </button>
                </div>
            ) : (
                <div className="space-y-8">
                    {!data && !loading && !error ? (
                        <div className="bg-white rounded-2xl border-2 border-dashed border-gray-100 p-16 text-center flex flex-col items-center justify-center">
                            <div className="w-20 h-20 bg-gray-50 rounded-full flex items-center justify-center text-4xl mb-6 grayscale opacity-60">
                                📈
                            </div>
                            <h3 className="text-xl font-bold text-gray-800 mb-2">Generate P&L Report</h3>
                            <p className="text-gray-500 max-w-xs mx-auto leading-relaxed">
                                Select a date range above and click the <span className="text-green-600 font-bold">Go</span> button to view metrics.
                            </p>
                        </div>
                    ) : (
                        <div className="space-y-8">
                            {/* Row 1: Core Summary */}
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                                <div className="bg-white rounded-2xl border border-gray-100 p-8 flex items-center gap-4 shadow-sm">
                                    <div className="p-3 bg-blue-50 text-blue-500 rounded-xl text-3xl border border-blue-100">💰</div>
                                    <div>
                                        <p className="text-xs font-bold uppercase tracking-widest text-gray-400 mb-1">Total Revenue</p>
                                        {loading ? <Skeleton className="h-8 w-32" /> : <h3 className="text-3xl font-bold text-gray-900">{formatCurrency(data?.totalRevenue)}</h3>}
                                    </div>
                                </div>

                                <div className="bg-white rounded-2xl border border-gray-100 p-8 flex items-center gap-4 shadow-sm">
                                    <div className="p-3 bg-red-50 text-red-500 rounded-xl text-3xl border border-red-100">📉</div>
                                    <div>
                                        <p className="text-xs font-bold uppercase tracking-widest text-gray-400 mb-1">Total Expense</p>
                                        {loading ? <Skeleton className="h-8 w-32" /> : <h3 className="text-3xl font-bold text-gray-900">{formatCurrency(data?.totalExpense)}</h3>}
                                    </div>
                                </div>

                                <div className="bg-white rounded-2xl border border-gray-100 p-8 flex items-center gap-4 shadow-sm">
                                    <div className="p-3 bg-green-50 text-green-500 rounded-xl text-3xl border border-green-100">⚖️</div>
                                    <div>
                                        <p className="text-xs font-bold uppercase tracking-widest text-gray-400 mb-1">Net Profit</p>
                                        {loading ? <Skeleton className="h-8 w-32" /> : <h3 className={`text-3xl font-bold ${(data?.netProfit ?? 0) >= 0 ? "text-green-600" : "text-red-600"}`}>{formatCurrency(data?.netProfit)}</h3>}
                                    </div>
                                </div>
                            </div>

                            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                                {/* Row 2: Cash Flow */}
                                <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
                                    <div className="px-6 py-4 bg-gray-50/50 border-b border-gray-100 flex justify-between items-center">
                                        <h4 className="text-xs font-bold uppercase tracking-widest text-gray-500">Cash-Based Movements</h4>
                                        <span className="text-xl grayscale opacity-50">💵</span>
                                    </div>
                                    <div className="p-8 grid grid-cols-3 gap-6">
                                        <div>
                                            <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Revenue</p>
                                            {loading ? <Skeleton className="h-6 w-20" /> : <p className="text-xl font-bold text-gray-800">{formatCurrency(data?.cashRevenue)}</p>}
                                        </div>
                                        <div>
                                            <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Expense</p>
                                            {loading ? <Skeleton className="h-6 w-20" /> : <p className="text-xl font-bold text-red-600">{formatCurrency(data?.cashExpense)}</p>}
                                        </div>
                                        <div className="text-right">
                                            <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Net Cash</p>
                                            {loading ? <Skeleton className="h-6 w-20 ml-auto" /> : <p className={`text-xl font-bold ${(data?.netCash ?? 0) >= 0 ? "text-green-600" : "text-red-600"}`}>{formatCurrency(data?.netCash)}</p>}
                                        </div>
                                    </div>
                                </div>

                                {/* Row 3 equivalent: Bank Flow */}
                                <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
                                    <div className="px-6 py-4 bg-gray-50/50 border-b border-gray-100 flex justify-between items-center">
                                        <h4 className="text-xs font-bold uppercase tracking-widest text-gray-500">Bank-Based Movements</h4>
                                        <span className="text-xl grayscale opacity-50">🏦</span>
                                    </div>
                                    <div className="p-8 grid grid-cols-3 gap-6">
                                        <div>
                                            <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Revenue</p>
                                            {loading ? <Skeleton className="h-6 w-20" /> : <p className="text-xl font-bold text-gray-800">{formatCurrency(data?.bankRevenue)}</p>}
                                        </div>
                                        <div>
                                            <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Expense</p>
                                            {loading ? <Skeleton className="h-6 w-20" /> : <p className="text-xl font-bold text-red-600">{formatCurrency(data?.bankExpense)}</p>}
                                        </div>
                                        <div className="text-right">
                                            <p className="text-[10px] font-bold text-gray-400 uppercase mb-1">Net Bank</p>
                                            {loading ? <Skeleton className="h-6 w-20 ml-auto" /> : <p className={`text-xl font-bold ${(data?.netBank ?? 0) >= 0 ? "text-green-600" : "text-red-600"}`}>{formatCurrency(data?.netBank)}</p>}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
