"use client";

import React from "react";
import { useSubscription } from "@/context/SubscriptionContext";
import { useRouter } from "next/navigation";

export default function SuspensionOverlay() {
    const { subscriptionStatus } = useSubscription();

    if (subscriptionStatus !== "SUSPENDED") return null;

    return (
        <div className="fixed inset-0 bg-white/90 backdrop-blur-sm z-[9999] flex items-center justify-center p-4">
            <div className="bg-white shadow-2xl rounded-2xl p-10 max-w-lg w-full border-2 border-red-500 text-center animate-in fade-in zoom-in duration-300">
                <div className="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-6 text-red-600">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-12 h-12">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
                    </svg>
                </div>

                <h1 className="text-3xl font-extrabold text-gray-900 mb-2">Account Suspended</h1>
                <p className="text-gray-600 mb-8 leading-relaxed">
                    Access to your school's operational features has been suspended due to an issue with your subscription.
                    Please review your subscription status or contact support.
                </p>

                <div className="flex flex-col gap-3">
                    <button
                        onClick={() => window.location.href = "/subscription"}
                        className="w-full bg-blue-600 text-white font-bold py-3 rounded-xl hover:bg-blue-700 transition-colors shadow-lg shadow-blue-200"
                    >
                        Go to Subscription & Billing
                    </button>
                    <p className="text-sm text-gray-400 mt-4">
                        Billing and administration pages remain accessible.
                    </p>
                </div>
            </div>
        </div>
    );
}
