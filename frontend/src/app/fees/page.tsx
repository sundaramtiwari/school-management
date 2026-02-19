"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Skeleton } from "@/components/ui/Skeleton";
import { useSession } from "@/context/SessionContext";
import { useAuth } from "@/context/AuthContext";

export default function FeesDashboard() {
    const { user } = useAuth();
    const { currentSession, isSessionLoading: sessionLoading } = useSession();

    const canManageFees = user?.role === "ACCOUNTANT" || user?.role === "SCHOOL_ADMIN" || user?.role === "SUPER_ADMIN";
    const [stats, setStats] = useState({
        todayCollection: 0,
        pendingDues: 0,
        totalStudents: 0
    });
    const [recentPayments, setRecentPayments] = useState<RecentPayment[]>([]);
    const [loading, setLoading] = useState(true);

    const loadStats = useCallback(async () => {
        if (!currentSession?.id) return;
        try {
            setLoading(true);
            const [statsRes, paymentsRes] = await Promise.all([
                api.get(`/api/fees/summary/stats?sessionId=${currentSession.id}`),
                api.get("/api/fees/payments/recent")
            ]);
            setStats(statsRes.data);
            setRecentPayments(paymentsRes.data || []);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    }, [currentSession?.id]);

    useEffect(() => {
        if (!sessionLoading && currentSession) {
            loadStats();
        }
    }, [sessionLoading, currentSession, loadStats]);

    const cards = [
        { label: "Today's Collection", value: stats.todayCollection, prefix: "₹", color: "text-green-600", bg: "bg-green-50" },
        { label: "Pending Dues", value: stats.pendingDues, prefix: "₹", color: "text-red-600", bg: "bg-red-50" },
        { label: "Total Students", value: stats.totalStudents, prefix: "", color: "text-blue-600", bg: "bg-blue-50" },
    ];

    return (
        <div className="mx-auto px-6 py-6 space-y-8">
            <header className="flex justify-between items-end">
                <div>
                    <h1 className="text-lg font-semibold">Fee Dashboard</h1>
                    <p className="text-gray-500 text-base mt-1">Institutional financial overview and collection status for <span className="text-blue-600 font-bold">{currentSession?.name || "current session"}</span>.</p>
                </div>
                <div className="flex gap-3">
                    {canManageFees && (
                        <>
                            <button onClick={() => window.location.href = '/fees/structures'} className="px-5 py-2.5 bg-white border border-gray-300 rounded-md font-medium hover:bg-gray-50 text-base">
                                Edit Structures
                            </button>
                            <button onClick={() => window.location.href = '/fees/collect'} className="px-5 py-2.5 bg-blue-600 text-white rounded-md font-medium hover:bg-blue-700 text-base">
                                Collect Fees
                            </button>
                        </>
                    )}
                </div>
            </header>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-4 mb-6">
                {cards.map((card, i) => (
                    <div key={i} className="bg-white rounded-lg shadow border border-gray-100 p-6">
                        <h3 className="text-lg font-semibold mb-4">{card.label}</h3>
                        {loading ? (
                            <Skeleton className="h-10 w-32" />
                        ) : (
                            <p className={`text-4xl font-black ${card.color}`}>
                                {card.prefix} {card.value.toLocaleString()}
                            </p>
                        )}
                        <div className={`mt-4 h-1 w-full rounded-full ${card.bg}`}>
                            <div className={`h-full rounded-full ${card.color.replace('text', 'bg')}`} style={{ width: '60%' }}></div>
                        </div>
                    </div>
                ))}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <div className="bg-white rounded-lg shadow border border-gray-100 p-6 space-y-4">
                    <h3 className="text-lg font-semibold">Recent Collections</h3>
                    <div className="space-y-3">
                        {recentPayments.map(p => (
                            <div key={p.id} className="flex justify-between items-center p-4 bg-gray-50 rounded-lg border border-gray-100 hover:bg-white">
                                <div>
                                    <p className="font-bold text-gray-800">Student ID #{p.studentId}</p>
                                    <p className="text-xs text-gray-400 font-medium uppercase tracking-tighter">
                                        {p.mode} • {new Date(p.paymentDate).toLocaleDateString()}
                                    </p>
                                </div>
                                <p className="font-black text-green-600">₹ {p.amountPaid.toLocaleString("en-IN")}</p>
                            </div>
                        ))}
                        {recentPayments.length === 0 && !loading && (
                            <div className="p-8 text-center text-gray-500 bg-white rounded-lg shadow border border-gray-100">
                                No recent transactions recorded.
                            </div>
                        )}
                    </div>
                    <button disabled className="w-full py-2 text-sm font-bold text-gray-400 cursor-not-allowed italic">
                        View All Transactions (Coming soon)
                    </button>
                </div>

                <div className="bg-white rounded-lg shadow border border-gray-100 p-6 flex flex-col justify-center items-center text-center space-y-4">
                    <div className="w-20 h-20 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center text-3xl shadow-inner">
                        📊
                    </div>
                    <div>
                        <h3 className="text-lg font-semibold">Collection Reports</h3>
                        <p className="text-base text-gray-500 mt-1 max-w-xs">Generate monthly, quarterly, or annual fee collection reports in PDF or Excel.</p>
                    </div>
                    <button disabled className="px-8 py-3 bg-gray-400 text-white rounded-md font-medium cursor-not-allowed opacity-70">
                        Generate Report (Coming soon)
                    </button>
                </div>
            </div>
        </div>
    );
}
    type RecentPayment = {
        id: number;
        studentId: number;
        mode: string;
        paymentDate: string;
        amountPaid: number;
    };
