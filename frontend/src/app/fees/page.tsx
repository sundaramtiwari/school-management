"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Skeleton } from "@/components/ui/Skeleton";

export default function FeesDashboard() {
    const [stats, setStats] = useState({
        todayCollection: 0,
        pendingDues: 0,
        totalStudents: 0
    });
    const [recentPayments, setRecentPayments] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function loadStats() {
            try {
                setLoading(true);
                const [statsRes, paymentsRes] = await Promise.all([
                    api.get("/api/fees/summary/stats?session=2024-25"),
                    api.get("/api/fees/payments/recent")
                ]);
                setStats(statsRes.data);
                setRecentPayments(paymentsRes.data || []);
            } catch (err) {
                console.error(err);
            } finally {
                setLoading(false);
            }
        }
        loadStats();
    }, []);

    const cards = [
        { label: "Today's Collection", value: stats.todayCollection, prefix: "₹", color: "text-green-600", bg: "bg-green-50" },
        { label: "Pending Dues", value: stats.pendingDues, prefix: "₹", color: "text-red-600", bg: "bg-red-50" },
        { label: "Total Students", value: stats.totalStudents, prefix: "", color: "text-blue-600", bg: "bg-blue-50" },
    ];

    return (
        <div className="space-y-8">
            <header className="flex justify-between items-end">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Fee Dashboard</h1>
                    <p className="text-gray-500 mt-1">Institutional financial overview and collection status.</p>
                </div>
                <div className="flex gap-3">
                    <button onClick={() => window.location.href = '/fees/structures'} className="px-5 py-2.5 bg-white border rounded-xl font-bold shadow-sm hover:bg-gray-50 transition-all text-gray-700">
                        Edit Structures
                    </button>
                    <button onClick={() => window.location.href = '/fees/collect'} className="px-5 py-2.5 bg-blue-600 text-white rounded-xl font-bold shadow-lg hover:bg-blue-700 transition-all">
                        Collect Fees
                    </button>
                </div>
            </header>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {cards.map((card, i) => (
                    <div key={i} className="bg-white p-8 rounded-2xl border shadow-sm transition-transform hover:scale-[1.02]">
                        <h3 className="text-xs font-black uppercase tracking-widest text-gray-400 mb-4">{card.label}</h3>
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
                <div className="bg-white p-8 rounded-2xl border shadow-sm space-y-4">
                    <h3 className="text-xl font-bold text-gray-800">Recent Collections</h3>
                    <div className="space-y-3">
                        {recentPayments.map(p => (
                            <div key={p.id} className="flex justify-between items-center p-4 bg-gray-50 rounded-xl border border-transparent hover:border-blue-200 hover:bg-white transition-all">
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
                            <div className="p-8 text-center text-gray-400 italic bg-gray-50 rounded-xl border border-dashed">
                                No recent transactions recorded.
                            </div>
                        )}
                    </div>
                    <button className="w-full py-2 text-sm font-bold text-blue-600 hover:underline">View All Transactions</button>
                </div>

                <div className="bg-white p-8 rounded-2xl border shadow-sm flex flex-col justify-center items-center text-center space-y-4">
                    <div className="w-20 h-20 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center text-3xl shadow-inner">
                        📊
                    </div>
                    <div>
                        <h3 className="text-xl font-bold text-gray-800">Collection Reports</h3>
                        <p className="text-sm text-gray-400 mt-1 max-w-xs">Generate monthly, quarterly, or annual fee collection reports in PDF or Excel.</p>
                    </div>
                    <button className="px-8 py-3 bg-gray-900 text-white rounded-xl font-bold shadow-lg hover:bg-black transition-all">
                        Generate Report
                    </button>
                </div>
            </div>
        </div>
    );
}
