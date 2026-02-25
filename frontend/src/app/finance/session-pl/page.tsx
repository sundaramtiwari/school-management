"use client";

import { useState, useEffect, useCallback } from "react";
import { financeApi, SessionPLData } from "@/lib/financeApi";
import { useToast } from "@/components/ui/Toast";
import { Skeleton } from "@/components/ui/Skeleton";

export default function SessionPLPage() {
    const { showToast } = useToast();
    const [loading, setLoading] = useState(true);
    const [data, setData] = useState<SessionPLData | null>(null);
    const [error, setError] = useState<string | null>(null);

    const fetchData = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const result = await financeApi.getSessionPL();
            setData(result);
        } catch (err: any) {
            console.error("Error fetching session P&L:", err);
            setError(err.message || "Failed to fetch session P&L data");
            showToast(err.message || "Failed to fetch session P&L data", "error");
        } finally {
            setLoading(false);
        }
    }, [showToast]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const formatCurrency = (amount: number = 0) => {
        return `₹${amount.toLocaleString("en-IN")}`;
    };

    return (
        <div className="max-w-7xl mx-auto space-y-8">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-gray-900">Session P&L</h1>
                    {loading ? (
                        <Skeleton className="h-6 w-48 mt-2" />
                    ) : (
                        <div className="mt-2 inline-flex items-center px-4 py-1.5 bg-blue-100 text-blue-700 rounded-full text-sm font-black uppercase tracking-widest border border-blue-200">
                            Session: {data?.sessionName || "Active Session"}
                        </div>
                    )}
                </div>
                <button
                    onClick={fetchData}
                    disabled={loading}
                    className="px-6 py-2 bg-gray-900 text-white rounded-xl font-bold hover:bg-gray-800 transition-colors disabled:opacity-50"
                >
                    Refresh Data
                </button>
            </div>

            {error ? (
                <div className="bg-red-50 border border-red-200 rounded-2xl p-8 text-center">
                    <div className="text-4xl mb-4">⚠️</div>
                    <h3 className="text-lg font-bold text-red-800 mb-2">Data Fetch Failed</h3>
                    <p className="text-red-600 mb-6">{error}</p>
                    <button
                        onClick={fetchData}
                        className="px-6 py-2 bg-red-600 text-white rounded-xl font-bold hover:bg-red-700 transition-colors"
                    >
                        Retry Fetch
                    </button>
                </div>
            ) : (
                <div className="space-y-8">
                    {/* Row 1: Core Summary */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <div className="bg-blue-50/50 rounded-2xl border border-blue-100 p-8 flex items-center gap-4">
                            <div className="p-3 bg-blue-100 text-blue-600 rounded-xl text-3xl">💰</div>
                            <div>
                                <p className="text-xs font-black uppercase tracking-widest text-blue-600/70 mb-1">Total Revenue</p>
                                {loading ? <Skeleton className="h-8 w-32" /> : <h3 className="text-3xl font-black text-blue-700">{formatCurrency(data?.totalRevenue)}</h3>}
                            </div>
                        </div>

                        <div className="bg-red-50/50 rounded-2xl border border-red-100 p-8 flex items-center gap-4">
                            <div className="p-3 bg-red-100 text-red-600 rounded-xl text-3xl">📉</div>
                            <div>
                                <p className="text-xs font-black uppercase tracking-widest text-red-600/70 mb-1">Total Expense</p>
                                {loading ? <Skeleton className="h-8 w-32" /> : <h3 className="text-3xl font-black text-red-700">{formatCurrency(data?.totalExpense)}</h3>}
                            </div>
                        </div>

                        <div className="bg-green-50/50 rounded-2xl border border-green-100 p-8 flex items-center gap-4">
                            <div className="p-3 bg-green-100 text-green-600 rounded-xl text-3xl">⚖️</div>
                            <div>
                                <p className="text-xs font-black uppercase tracking-widest text-green-600/70 mb-1">Net Profit</p>
                                {loading ? <Skeleton className="h-8 w-32" /> : <h3 className={`text-3xl font-black ${(data?.netProfit ?? 0) >= 0 ? "text-green-700" : "text-red-700"}`}>{formatCurrency(data?.netProfit)}</h3>}
                            </div>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                        {/* Row 2: Cash Flow */}
                        <div className="bg-white rounded-2xl border shadow-sm overflow-hidden">
                            <div className="px-6 py-4 bg-amber-50 border-b border-amber-100 flex justify-between items-center">
                                <h4 className="text-xs font-black uppercase tracking-widest text-amber-700">Cash Flow</h4>
                                <span className="text-xl">💵</span>
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
                                    {loading ? <Skeleton className="h-6 w-20 ml-auto" /> : <p className={`text-xl font-black ${(data?.netCash ?? 0) >= 0 ? "text-green-600" : "text-red-600"}`}>{formatCurrency(data?.netCash)}</p>}
                                </div>
                            </div>
                        </div>

                        {/* Subtle Divider for Desktop */}
                        <div className="hidden lg:block w-px bg-gray-100 self-stretch my-4"></div>

                        {/* Row 3 equivalent: Bank Flow */}
                        <div className="bg-white rounded-2xl border shadow-sm overflow-hidden">
                            <div className="px-6 py-4 bg-purple-50 border-b border-purple-100 flex justify-between items-center">
                                <h4 className="text-xs font-black uppercase tracking-widest text-purple-700">Bank Flow</h4>
                                <span className="text-xl">🏦</span>
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
                                    {loading ? <Skeleton className="h-6 w-20 ml-auto" /> : <p className={`text-xl font-black ${(data?.netBank ?? 0) >= 0 ? "text-green-600" : "text-red-600"}`}>{formatCurrency(data?.netBank)}</p>}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
