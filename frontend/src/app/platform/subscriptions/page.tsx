"use client";

import React, { useEffect, useState, useCallback } from "react";
import { platformApi } from "@/lib/platformApi";
import { platformSubscriptionApi, AdminSubscriptionUsageDto, SubscriptionStatus } from "@/lib/subscriptionApi";
import {
    StartTrialModal, ActivateModal, RecordPaymentModal,
    ExtendModal, SuspendModal, ReactivateModal, UpgradeDowngradeModal
} from "@/components/subscription/SubscriptionModals";
import Link from "next/link";

type SchoolRow = {
    id: number;
    name: string;
    isActive: boolean;
    usage: AdminSubscriptionUsageDto | null;
    isLoading: boolean;
};

export default function PlatformSubscriptionDashboard() {
    const [schools, setSchools] = useState<SchoolRow[]>([]);
    const [isLoadingInitial, setIsLoadingInitial] = useState(true);

    // Modal state
    const [modal, setModal] = useState<{
        type: "TRIAL" | "ACTIVATE" | "PAYMENT" | "EXTEND_TRIAL" | "EXTEND_SUB" | "SUSPEND" | "REACTIVATE" | "UPGRADE" | "DOWNGRADE" | null,
        schoolId?: number,
        schoolName?: string,
        subscriptionId?: number
    }>({ type: null });

    const fetchUsage = useCallback(async (schoolId: number) => {
        try {
            const res = await platformSubscriptionApi.getAdminUsage(schoolId);
            return res.data;
        } catch (e) {
            console.error(`Failed usage for school ${schoolId}`, e);
            return null;
        }
    }, []);

    const loadData = useCallback(async () => {
        setIsLoadingInitial(true);
        try {
            const res = await platformApi.get("/api/schools?page=0&size=100");
            const rawSchools = res.data.content || res.data;

            const rows: SchoolRow[] = rawSchools.map((s: any) => ({
                id: s.id,
                name: s.name,
                isActive: s.active !== false, // Default to true if missing
                usage: null,
                isLoading: true
            }));
            setSchools(rows); // Show loading rows immediately

            const schoolIds = rawSchools.map((s: any) => s.id);
            if (schoolIds.length > 0) {
                const bulkRes = await platformSubscriptionApi.getAdminUsageBulk(schoolIds);
                const usageMap = bulkRes.data;
                setSchools(rows.map(r => ({
                    ...r,
                    usage: usageMap[r.id] || null,
                    isLoading: false
                })));
            }

        } catch (e) {
            console.error("Dashboard failed", e);
        } finally {
            setIsLoadingInitial(false);
        }
    }, [fetchUsage]);

    useEffect(() => {
        loadData();
    }, [loadData]);

    const handleSuccess = (schoolId: number) => {
        // Just refresh the specific row's usage
        fetchUsage(schoolId).then(usage => {
            setSchools(current => current.map(s => s.id === schoolId ? { ...s, usage, isLoading: false } : s));
        });
    };

    const getStatusBadge = (status: SubscriptionStatus | undefined) => {
        const colors: Record<string, string> = {
            TRIAL: "bg-blue-100 text-blue-700 border-blue-200",
            ACTIVE: "bg-green-100 text-green-700 border-green-200",
            PAST_DUE: "bg-orange-100 text-orange-700 border-orange-200",
            SUSPENDED: "bg-red-100 text-red-700 border-red-200",
            NO_PLAN: "bg-gray-100 text-gray-500 border-gray-200",
            null: "bg-gray-100 text-gray-500 border-gray-200"
        };
        const s = status || "NO_PLAN";
        return <span className={`px-2 py-0.5 rounded text-[10px] font-bold border uppercase ${colors[s]}`}>{s}</span>;
    };

    if (isLoadingInitial && schools.length === 0) {
        return <div className="p-8 text-gray-400">Initialising control plane...</div>;
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-gray-800">Subscription Control Plane</h1>
                <button onClick={loadData} className="px-4 py-2 bg-white border rounded-lg text-sm font-medium hover:bg-gray-50 shadow-sm">
                    🔄 Global Sync
                </button>
            </div>

            <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
                <table className="w-full text-sm">
                    <thead>
                        <tr className="bg-gray-50 border-b text-gray-500 text-[10px] font-bold uppercase tracking-wider">
                            <th className="px-6 py-4 text-left">School / Plan</th>
                            <th className="px-6 py-4 text-center">Status</th>
                            <th className="px-6 py-4 text-center">Usage</th>
                            <th className="px-6 py-4 text-center">Exp. Date</th>
                            <th className="px-6 py-4 text-center">Grace End</th>
                            <th className="px-6 py-4 text-center">Days left</th>
                            <th className="px-6 py-4 text-right">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                        {schools.map(row => (
                            <tr key={row.id} className="hover:bg-gray-50 group transition-colors">
                                <td className="px-6 py-4">
                                    <div className="flex items-center gap-2">
                                        <div className={`font-extrabold transition-colors uppercase tracking-tight ${row.isActive ? 'text-gray-900 group-hover:text-blue-600' : 'text-gray-400 line-through'}`}>
                                            {row.name}
                                        </div>
                                        {!row.isActive && (
                                            <span className="bg-red-50 text-red-500 border border-red-100 px-1.5 py-0.5 rounded-[4px] text-[8px] font-black uppercase tracking-tighter shadow-sm">
                                                Locked
                                            </span>
                                        )}
                                    </div>
                                    <div className="text-[10px] text-gray-400 font-mono tracking-tighter">{row.usage?.planName || "No active plan"}</div>
                                </td>
                                <td className="px-6 py-4 text-center">
                                    {row.isLoading ? <span className="text-[10px] text-gray-300 animate-pulse">SYNCING</span> : getStatusBadge(row.usage?.subscriptionStatus)}
                                </td>
                                <td className="px-6 py-4 text-center">
                                    {!row.usage ? "—" : (
                                        <div className="flex flex-col items-center">
                                            <div className="text-[10px] font-bold text-gray-600">{row.usage.activeStudents} <span className="text-gray-300">/</span> {row.usage.studentCap}</div>
                                            <div className="text-[9px] font-mono text-gray-400 mb-1">{row.usage.usagePercent}%</div>
                                            <div className="w-16 bg-gray-100 h-1 rounded-full overflow-hidden shadow-inner">
                                                <div
                                                    className={`h-full transition-all duration-500 ${Number(row.usage.usagePercent) > 90 ? "bg-red-500" : Number(row.usage.usagePercent) > 75 ? "bg-orange-500" : "bg-blue-500"}`}
                                                    style={{ width: `${Math.min(Number(row.usage.usagePercent), 100)}%` }}
                                                />
                                            </div>
                                        </div>
                                    )}
                                </td>
                                <td className="px-6 py-4 text-center font-mono text-[10px] text-gray-500 tracking-tighter">
                                    {row.usage?.expiryDate ?? "—"}
                                </td>
                                <td className="px-6 py-4 text-center font-mono text-[10px] text-gray-500 tracking-tighter">
                                    {row.usage?.graceEndDate ?? "—"}
                                </td>
                                <td className="px-6 py-4 text-center">
                                    {row.usage ? (
                                        <div className={`px-2 py-0.5 rounded text-[10px] font-bold border inline-block font-mono ${row.usage.daysToExpiry <= 7 ? "bg-red-50 text-red-600 border-red-100" : "bg-gray-50 text-gray-600 border-gray-100"}`}>
                                            {row.usage.daysToExpiry} d
                                        </div>
                                    ) : "—"}
                                </td>
                                <td className="px-6 py-4 text-right">
                                    <div className="flex gap-2 justify-end">
                                        <AdminActions
                                            row={row}
                                            onAction={(type) => setModal({
                                                type,
                                                schoolId: row.id,
                                                schoolName: row.name,
                                                subscriptionId: row.usage?.subscriptionId
                                            })}
                                        />
                                        <Link
                                            href={`/platform/subscriptions/${row.id}`}
                                            className="p-1.5 text-blue-600 hover:bg-blue-50 rounded transition-colors"
                                            title="View Detail"
                                        >
                                            👁️
                                        </Link>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Modals Injection */}
            <StartTrialModal
                isOpen={modal.type === "TRIAL"}
                onClose={() => setModal({ type: null })}
                onSuccess={() => handleSuccess(modal.schoolId!)}
                schoolId={modal.schoolId}
                schoolName={modal.schoolName}
            />
            <ActivateModal
                isOpen={modal.type === "ACTIVATE"}
                onClose={() => setModal({ type: null })}
                onSuccess={() => handleSuccess(modal.schoolId!)}
                subscriptionId={modal.subscriptionId}
                schoolName={modal.schoolName}
            />
            <RecordPaymentModal
                isOpen={modal.type === "PAYMENT"}
                onClose={() => setModal({ type: null })}
                onSuccess={() => handleSuccess(modal.schoolId!)}
                subscriptionId={modal.subscriptionId}
                schoolName={modal.schoolName}
            />
            <ExtendModal
                isOpen={modal.type === "EXTEND_TRIAL" || modal.type === "EXTEND_SUB"}
                onClose={() => setModal({ type: null })}
                onSuccess={() => handleSuccess(modal.schoolId!)}
                subscriptionId={modal.subscriptionId}
                schoolName={modal.schoolName}
                isTrial={modal.type === "EXTEND_TRIAL"}
            />
            <SuspendModal
                isOpen={modal.type === "SUSPEND"}
                onClose={() => setModal({ type: null })}
                onSuccess={() => handleSuccess(modal.schoolId!)}
                subscriptionId={modal.subscriptionId}
                schoolName={modal.schoolName}
            />
            <ReactivateModal
                isOpen={modal.type === "REACTIVATE"}
                onClose={() => setModal({ type: null })}
                onSuccess={() => handleSuccess(modal.schoolId!)}
                subscriptionId={modal.subscriptionId}
                schoolName={modal.schoolName}
            />
            <UpgradeDowngradeModal
                isOpen={modal.type === "UPGRADE" || modal.type === "DOWNGRADE"}
                onClose={() => setModal({ type: null })}
                onSuccess={() => handleSuccess(modal.schoolId!)}
                subscriptionId={modal.subscriptionId}
                schoolName={modal.schoolName}
                isUpgrade={modal.type === "UPGRADE"}
            />
        </div>
    );
}

function AdminActions({ row, onAction }: { row: SchoolRow, onAction: (type: any) => void }) {
    const status = row.usage?.subscriptionStatus;

    if (!row.usage || status === "NO_PLAN" || !status) {
        return <button onClick={() => onAction("TRIAL")} className="text-[10px] bg-blue-600 text-white px-2 py-1 rounded font-bold hover:bg-blue-700">START TRIAL</button>;
    }

    return (
        <div className="flex gap-1.5 opacity-60 group-hover:opacity-100 transition-opacity">
            {status === "TRIAL" && (
                <>
                    <ActionButton onClick={() => onAction("ACTIVATE")} label="ACTIVATE" color="green" />
                    <ActionButton onClick={() => onAction("EXTEND_TRIAL")} label="+ TRIAL" color="orange" />
                    <ActionButton onClick={() => onAction("UPGRADE")} label="UPGRADE" color="indigo" />
                    <ActionButton onClick={() => onAction("SUSPEND")} label="SUSPEND" color="red" />
                </>
            )}
            {status === "ACTIVE" && (
                <>
                    <ActionButton onClick={() => onAction("UPGRADE")} label="UPGRADE" color="indigo" />
                    <ActionButton onClick={() => onAction("DOWNGRADE")} label="DOWNGRADE" color="purple" />
                    <ActionButton onClick={() => onAction("EXTEND_SUB")} label="+ SUB" color="orange" />
                    <ActionButton onClick={() => onAction("PAYMENT")} label="$ PAY" color="blue" />
                    <ActionButton onClick={() => onAction("SUSPEND")} label="SUSPEND" color="red" />
                </>
            )}
            {status === "PAST_DUE" && (
                <>
                    <ActionButton onClick={() => onAction("PAYMENT")} label="$ PAY" color="blue" />
                    <ActionButton onClick={() => onAction("EXTEND_SUB")} label="+ Grace" color="orange" />
                    <ActionButton onClick={() => onAction("SUSPEND")} label="SUSPEND" color="red" />
                </>
            )}
            {status === "SUSPENDED" && (
                <>
                    <ActionButton onClick={() => onAction("REACTIVATE")} label="RE-ACTIVATE" color="green" />
                    <ActionButton onClick={() => onAction("PAYMENT")} label="$ PAY" color="blue" />
                </>
            )}
        </div>
    );
}

function ActionButton({ onClick, label, color }: { onClick: () => void, label: string, color: string }) {
    const colorClasses: Record<string, string> = {
        blue: "bg-blue-50 text-blue-600 border-blue-200 hover:bg-blue-600 hover:text-white",
        green: "bg-green-50 text-green-600 border-green-200 hover:bg-green-600 hover:text-white",
        orange: "bg-orange-50 text-orange-600 border-orange-200 hover:bg-orange-600 hover:text-white",
        red: "bg-red-50 text-red-600 border-red-200 hover:bg-red-600 hover:text-white",
        indigo: "bg-indigo-50 text-indigo-600 border-indigo-200 hover:bg-indigo-600 hover:text-white",
        purple: "bg-purple-50 text-purple-600 border-purple-200 hover:bg-purple-600 hover:text-white",
    };
    return (
        <button
            onClick={onClick}
            className={`text-[9px] font-extrabold px-2 py-1 rounded border transition-colors ${colorClasses[color]}`}
        >
            {label}
        </button>
    );
}
