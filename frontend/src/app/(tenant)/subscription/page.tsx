"use client";

import React, { useEffect, useState } from "react";
import { useSubscription } from "@/context/SubscriptionContext";
import { subscriptionApi, SubscriptionDto } from "@/lib/subscriptionApi";

export default function SchoolSubscriptionPage() {
    const {
        subscriptionStatus,
        usagePercent,
        usageWarningLevel,
        daysToExpiry,
        expiryWarningLevel,
        isLoading: isStatusLoading,
        refreshSubscription
    } = useSubscription();

    const [subscription, setSubscription] = useState<SubscriptionDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const res = await subscriptionApi.getMySubscription();
                setSubscription(res.data);
            } catch (error) {
                console.error("Failed to fetch subscription details:", error);
            } finally {
                setIsLoading(false);
            }
        };
        fetchData();
    }, []);

    if (isStatusLoading || isLoading) {
        return <div className="p-8 text-gray-400">Loading subscription...</div>;
    }

    // SUSPENDED VIEW
    if (subscriptionStatus === "SUSPENDED") {
        return <SuspensionOverlay />;
    }

    const usageColor =
        usageWarningLevel === "CRITICAL" ? "bg-red-500" :
            usageWarningLevel === "WARNING" ? "bg-yellow-500" :
                "bg-blue-600";

    const getStatusContent = () => {
        if (subscriptionStatus === "TRIAL") {
            return {
                label: `Trial ends on ${subscription?.trialEndDate}`,
                sub: `${daysToExpiry} days remaining`
            };
        }
        if (subscriptionStatus === "ACTIVE") {
            return {
                label: `Expires on ${subscription?.expiryDate}`,
                sub: `${daysToExpiry} days remaining`
            };
        }
        if (subscriptionStatus === "PAST_DUE") {
            return {
                label: `Expired on ${subscription?.expiryDate}`,
                sub: `Grace period ends on ${subscription?.graceEndDate}`
            };
        }
        return { label: "Unknown Status", sub: "" };
    };

    const statusInfo = getStatusContent();

    return (
        <div className="p-8 max-w-5xl mx-auto space-y-8">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-extrabold text-gray-900 tracking-tight">Institution Subscription</h1>
                <StatusBadge status={subscriptionStatus} />
            </div>

            {/* STATUS ALERT BAR */}
            {(usageWarningLevel !== "NONE" || expiryWarningLevel !== "NONE") && (
                <div className={`p-4 rounded-xl border flex items-center gap-3 ${usageWarningLevel === "CRITICAL" || expiryWarningLevel === "EXPIRED"
                        ? "bg-red-50 border-red-100 text-red-700"
                        : "bg-orange-50 border-orange-100 text-orange-700"
                    }`}>
                    <svg className="w-5 h-5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>
                    <span className="text-sm font-bold uppercase tracking-wide">
                        {usageWarningLevel === "CRITICAL" && "Capacity Critical: Increase student cap soon. "}
                        {usageWarningLevel === "WARNING" && "Capacity Warning: Nearing student limit. "}
                        {expiryWarningLevel === "EXPIRED" && "Service Expired: Payment required immediately. "}
                        {expiryWarningLevel === "CRITICAL_7" && "Expiring Soon: Less than 7 days left. "}
                        {expiryWarningLevel === "WARNING_30" && "Renewal Required: Expiring this month."}
                    </span>
                </div>
            )}

            {/* TOP CARDS */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">Current Plan</p>
                    <h3 className="text-xl font-extrabold text-gray-900">{subscription?.pricingPlanName}</h3>
                    <p className="text-xs text-blue-600 font-bold mt-2 bg-blue-50 inline-block px-2 py-0.5 rounded uppercase tracking-tighter">Yearly Billing</p>
                </div>

                <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">Timeline</p>
                    <h3 className="text-xl font-extrabold text-gray-900">{statusInfo.label}</h3>
                    <p className="text-xs text-orange-600 font-bold mt-2 uppercase tracking-tighter">{statusInfo.sub}</p>
                </div>

                <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">Student Capacity</p>
                    <div className="flex items-end gap-2">
                        <h3 className="text-xl font-extrabold text-gray-900">{usagePercent}%</h3>
                        <p className="text-[10px] text-gray-400 mb-1 font-bold">Consumed</p>
                    </div>
                    <div className="w-full bg-gray-100 rounded-full h-1.5 mt-4 overflow-hidden">
                        <div className={`h-full rounded-full transition-all duration-700 ${usageColor}`} style={{ width: `${Math.min(usagePercent, 100)}%` }} />
                    </div>
                </div>
            </div>

            {/* INFO PANEL */}
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                <div className="px-6 py-4 bg-gray-50 border-b">
                    <h2 className="text-sm font-bold text-gray-700">Contractual Limits</h2>
                </div>
                <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-8 text-sm">
                    <div className="space-y-4">
                        <div className="flex justify-between items-center group">
                            <span className="text-gray-500 font-medium">Daily Operations Limit</span>
                            <span className="font-bold text-gray-900 px-2 py-1 bg-gray-50 rounded group-hover:bg-blue-50 group-hover:text-blue-600 transition-colors">Enforced at 100% Student Cap</span>
                        </div>
                        <div className="flex justify-between items-center group">
                            <span className="text-gray-500 font-medium">Standard Grace period</span>
                            <span className="font-bold text-gray-900 px-2 py-1 bg-gray-50 rounded group-hover:bg-blue-50 group-hover:text-blue-600 transition-colors">{subscription?.gracePeriodDays} Days</span>
                        </div>
                    </div>
                    <div className="bg-blue-50/50 p-4 rounded-xl border border-blue-50 text-xs text-blue-700 leading-relaxed italic">
                        Note: For plan upgrades or billing disputes, please contact the platform administration. Automated renewals are not currently enabled.
                    </div>
                </div>
            </div>
        </div>
    );
}

function StatusBadge({ status }: { status: any }) {
    const colors: Record<string, string> = {
        TRIAL: "bg-blue-600 text-white",
        ACTIVE: "bg-green-600 text-white",
        PAST_DUE: "bg-orange-600 text-white",
        SUSPENDED: "bg-red-600 text-white"
    };
    return <span className={`px-4 py-1 rounded-full text-[10px] font-extrabold uppercase tracking-widest shadow-md ${colors[status] || 'bg-gray-400'}`}>{status}</span>;
}

function SuspensionOverlay() {
    return (
        <div className="min-h-[80vh] flex flex-col items-center justify-center p-8 text-center space-y-6">
            <div className="w-24 h-24 bg-red-100 rounded-full flex items-center justify-center animate-pulse">
                <svg className="w-12 h-12 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m0 0h.01M4.93 4.93l14.14 14.14M12 5V3m0 0H7m5 0h5M5 12H3m0 0v5m0-5V7m14 5h2m0 0v5m0-5V7" />
                </svg>
            </div>
            <div className="max-w-md">
                <h1 className="text-3xl font-black text-gray-900 mb-2">ACCESS SUSPENDED</h1>
                <p className="text-gray-500 font-medium leading-relaxed">Your institution's access has been administrative locked. This typically happens due to an expired subscription or a policy violation.</p>
            </div>
            <div className="bg-gray-50 p-6 rounded-2xl border border-gray-100 max-w-sm w-full">
                <p className="text-xs font-bold text-gray-400 uppercase mb-4 tracking-widest">Next Steps</p>
                <button
                    onClick={() => window.location.href = 'mailto:support@schoolmanagementsystem.com?subject=Reactivation Request'}
                    className="w-full bg-black text-white py-3 rounded-xl font-bold hover:bg-gray-800 transition-all shadow-lg shadow-gray-200"
                >
                    Contact Platform Support
                </button>
                <div className="mt-4 text-[10px] text-gray-400 font-medium">Please quote your school registration code in all correspondence.</div>
            </div>
        </div>
    );
}
