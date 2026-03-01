"use client";

import { useState } from "react";
import DailyOverview from "./components/DailyOverview";
import RangePLOverview from "./components/RangePLOverview";

type TabType = "daily" | "pl";

export default function FinancePage() {
    const [activeTab, setActiveTab] = useState<TabType>("daily");

    return (
        <div className="min-h-screen bg-gray-50/30 p-4 md:p-8">
            <div className="max-w-7xl mx-auto space-y-8">
                {/* Unified Header */}
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
                    <div className="space-y-1">
                        <h1 className="text-3xl font-extrabold text-gray-900 tracking-tight">Finance Reporting</h1>
                        <p className="text-gray-500 font-medium tracking-tight">Consolidated financial overview and accounting metrics</p>
                    </div>

                    {/* Tab Navigation */}
                    <div className="flex bg-white p-1 rounded-xl shadow-sm border border-gray-100 w-fit">
                        <button
                            onClick={() => setActiveTab("daily")}
                            className={`flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-bold transition-all ${activeTab === "daily"
                                ? "bg-gray-900 text-white shadow-lg"
                                : "text-gray-500 hover:text-gray-700 hover:bg-gray-50"
                                }`}
                        >
                            <span>🕒</span> Daily Cash
                        </button>
                        <button
                            onClick={() => setActiveTab("pl")}
                            className={`flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-bold transition-all ${activeTab === "pl"
                                ? "bg-gray-900 text-white shadow-lg"
                                : "text-gray-500 hover:text-gray-700 hover:bg-gray-50"
                                }`}
                        >
                            <span>📊</span> P&L Report
                        </button>
                    </div>
                </div>

                {/* Content Area */}
                <div className="bg-white rounded-3xl border border-gray-100 shadow-sm p-6 md:p-10 animate-in fade-in slide-in-from-bottom-2 duration-500">
                    <div className={activeTab === "daily" ? "" : "hidden"}>
                        <DailyOverview />
                    </div>
                    <div className={activeTab === "pl" ? "" : "hidden"}>
                        <RangePLOverview />
                    </div>
                </div>
            </div>

            {/* Print styles for the entire page */}
            <style jsx global>{`
                @media print {
                    .min-h-screen {
                        padding: 0 !important;
                        background: white !important;
                    }
                    .max-w-7xl {
                        max-width: 100% !important;
                    }
                    /* Hide non-essential print elements */
                    nav, button, input[type="date"], .bg-white.p-1.rounded-xl {
                        display: none !important;
                    }
                    .md\\:p-10 {
                        padding: 0 !important;
                    }
                    .shadow-sm, .rounded-3xl, .border {
                        border: none !important;
                        box-shadow: none !important;
                    }
                }
            `}</style>
        </div>
    );
}
