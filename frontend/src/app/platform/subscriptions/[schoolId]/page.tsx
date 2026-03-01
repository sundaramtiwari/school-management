"use client";

import React, { useEffect, useState, useMemo } from "react";
import { useParams, useRouter } from "next/navigation";
import {
    platformSubscriptionApi, AdminSubscriptionUsageDto,
    SubscriptionPaymentDto, SubscriptionEventDto, SubscriptionStatus
} from "@/lib/subscriptionApi";
import { platformApi } from "@/lib/platformApi";
import Link from "next/link";

export default function SubscriptionDetailPage() {
    const { schoolId } = useParams();
    const router = useRouter();
    const [usage, setUsage] = useState<AdminSubscriptionUsageDto | null>(null);
    const [payments, setPayments] = useState<SubscriptionPaymentDto[]>([]);
    const [events, setEvents] = useState<SubscriptionEventDto[]>([]);
    const [schoolName, setSchoolName] = useState("");
    const [isLoading, setIsLoading] = useState(true);

    const sid = Number(schoolId);

    useEffect(() => {
        if (!sid) return;

        const loadDetail = async () => {
            setIsLoading(true);
            try {
                // 1. Fetch school basic info
                const sres = await platformApi.get(`/api/schools/id/${sid}`);
                setSchoolName(sres.data.name);

                // 2. Fetch admin usage
                const ures = await platformSubscriptionApi.getAdminUsage(sid);
                setUsage(ures.data);

                if (ures.data.subscriptionId) {
                    // 3. Fetch history
                    const [pres, eres] = await Promise.all([
                        platformSubscriptionApi.getPaymentHistory(ures.data.subscriptionId),
                        platformSubscriptionApi.getEventHistory(ures.data.subscriptionId)
                    ]);
                    setPayments(pres.data);
                    setEvents(eres.data);
                }
            } catch (e) {
                console.error("Failed to load details", e);
            } finally {
                setIsLoading(false);
            }
        };

        loadDetail();
    }, [sid]);

    if (isLoading) return <div className="p-8 text-gray-400">Loading school subscription profile...</div>;

    return (
        <div className="max-w-6xl mx-auto space-y-8">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <button onClick={() => router.back()} className="p-2 hover:bg-gray-100 rounded-full transition-colors">←</button>
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900">{schoolName}</h1>
                        <p className="text-gray-500 text-sm">Subscription Audit & Controls</p>
                    </div>
                </div>
                {usage && <StatusBadge status={usage.subscriptionStatus} />}
            </div>

            {/* SECTION 1: OVERVIEW */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <StatCard label="Pricing Plan" value={usage?.planName || "NO PLAN"} />
                <StatCard label="Consumption" value={`${usage?.activeStudents} / ${usage?.studentCap} Students`} subValue={`${usage?.usagePercent}% Limit`} />
                <StatCard label="Time Left" value={`${usage?.daysToExpiry} Days`} subValue={`Expires ${usage?.expiryDate || "—"}`} />
                <StatCard label="Grace Period Ends" value={usage?.graceEndDate || "None"} />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                {/* SECTION 2: PAYMENT HISTORY */}
                <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
                    <div className="px-6 py-4 border-b bg-gray-50 flex justify-between items-center">
                        <h3 className="font-bold text-gray-700">Financial History</h3>
                        <span className="text-xs bg-gray-200 px-2 py-0.5 rounded text-gray-600 font-bold">{payments.length} Records</span>
                    </div>
                    <div className="p-0 overflow-x-auto min-h-[300px]">
                        <table className="w-full text-sm">
                            <thead className="bg-white border-b text-gray-400 text-[10px] uppercase font-bold tracking-wider">
                                <tr>
                                    <th className="px-6 py-3 text-left">Date</th>
                                    <th className="px-6 py-3 text-left">Amount</th>
                                    <th className="px-6 py-3 text-left">Type</th>
                                    <th className="px-6 py-3 text-left">Reference</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-50">
                                {payments.map(p => (
                                    <tr key={p.id} className="hover:bg-blue-50/30 transition-colors">
                                        <td className="px-6 py-3 text-gray-600">{p.paymentDate}</td>
                                        <td className="px-6 py-3 font-bold text-gray-900">₹{p.amount}</td>
                                        <td className="px-6 py-3">
                                            <span className={`px-1.5 py-0.5 rounded text-[9px] font-bold border ${p.type === 'UPGRADE_PRORATION' ? 'border-indigo-200 text-indigo-600' : 'border-green-200 text-green-600'}`}>
                                                {p.type}
                                            </span>
                                        </td>
                                        <td className="px-6 py-3 font-mono text-xs text-blue-600">{p.referenceNumber}</td>
                                    </tr>
                                ))}
                                {payments.length === 0 && <tr><td colSpan={4} className="p-8 text-center text-gray-300 italic">No payments recorded</td></tr>}
                            </tbody>
                        </table>
                    </div>
                </div>

                {/* SECTION 3: SUBSCRIPTION EVENTS */}
                <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
                    <div className="px-6 py-4 border-b bg-gray-50 flex justify-between items-center">
                        <h3 className="font-bold text-gray-700">Lifecycle Timeline</h3>
                        <span className="text-xs bg-gray-200 px-2 py-0.5 rounded text-gray-600 font-bold">{events.length} Events</span>
                    </div>
                    <div className="p-6 space-y-6 max-h-[500px] overflow-y-auto">
                        {events.map((e, idx) => (
                            <TimelineItem key={e.id} event={e} isLast={idx === events.length - 1} />
                        ))}
                        {events.length === 0 && <div className="text-center text-gray-300 italic py-10">No lifecycle events tracked</div>}
                    </div>
                </div>
            </div>
        </div>
    );
}

function StatCard({ label, value, subValue }: { label: string, value: string, subValue?: string }) {
    return (
        <div className="bg-white p-5 rounded-xl border shadow-sm">
            <div className="text-[10px] uppercase font-bold text-gray-400 tracking-wider mb-1">{label}</div>
            <div className="text-xl font-extrabold text-gray-900">{value}</div>
            {subValue && <div className="text-[10px] text-blue-600 font-bold mt-1 uppercase">{subValue}</div>}
        </div>
    );
}

function StatusBadge({ status }: { status?: SubscriptionStatus }) {
    const s = status || "NO_PLAN";
    const colors: Record<string, string> = {
        TRIAL: "bg-blue-600 text-white",
        ACTIVE: "bg-green-600 text-white",
        PAST_DUE: "bg-orange-600 text-white",
        SUSPENDED: "bg-red-600 text-white",
        NO_PLAN: "bg-gray-400 text-white"
    };
    return <span className={`px-4 py-1 rounded-full text-xs font-bold uppercase tracking-widest shadow-sm ${colors[s]}`}>{s}</span>;
}

function TimelineItem({ event, isLast }: { event: SubscriptionEventDto, isLast: boolean }) {
    return (
        <div className="flex gap-4 group">
            <div className="flex flex-col items-center">
                <div className="w-3 h-3 rounded-full bg-blue-500 ring-4 ring-blue-50 group-hover:ring-blue-100 transition-all flex-shrink-0" />
                {!isLast && <div className="w-0.5 flex-1 bg-gray-100 group-hover:bg-blue-100 transition-colors my-1" />}
            </div>
            <div className="flex-1 pb-6">
                <div className="flex justify-between items-start mb-1">
                    <span className="text-xs font-extrabold text-gray-800 uppercase tracking-tight">{event.type.replace(/_/g, " ")}</span>
                    <div className="flex flex-col items-end">
                        <span className="text-[9px] text-gray-400 font-mono tracking-tighter">{new Date(event.createdAt).toLocaleString()}</span>
                        {event.performedBy && <span className="text-[8px] text-blue-400 font-bold uppercase tracking-tighter">{event.performedBy}</span>}
                    </div>
                </div>
                <div className="text-sm text-gray-500 bg-gray-50 p-3 rounded-lg border border-gray-100">
                    <p className="font-medium text-gray-700 leading-relaxed italic">"{event.reason || "Automatic system update"}"</p>
                    {event.newStatus && (
                        <div className="mt-1 flex items-center gap-2 text-[10px] font-bold text-indigo-600 uppercase tracking-tight">
                            <span>{event.previousStatus || "START"}</span>
                            <span>→</span>
                            <span className="bg-indigo-600 text-white px-1.5 py-0.5 rounded shadow-sm">{event.newStatus}</span>
                        </div>
                    )}
                    {event.newExpiryDate && (
                        <div className="mt-2 flex items-center gap-2 text-[10px] font-bold text-blue-600 uppercase">
                            <span>Expiry: {event.previousExpiryDate || "—"}</span>
                            <span>→</span>
                            <span className="bg-blue-600 text-white px-1.5 py-0.5 rounded shadow-sm">{event.newExpiryDate}</span>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
