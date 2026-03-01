"use client";

import React, { useEffect, useState } from "react";
import { pricingPlanApi, platformSubscriptionApi, PricingPlanDto, UpgradePlanResponse } from "@/lib/subscriptionApi";
import { extractApiError } from "@/lib/api";

interface UpgradeModalProps {
    subscriptionId: number;
    currentPlanId: number;
    schoolName: string;
    onClose: () => void;
    onSuccess: () => void;
}

export default function UpgradeModal({ subscriptionId, currentPlanId, schoolName, onClose, onSuccess }: UpgradeModalProps) {
    const [plans, setPlans] = useState<PricingPlanDto[]>([]);
    const [selectedPlanId, setSelectedPlanId] = useState<number | "">("");
    const [preview, setPreview] = useState<UpgradePlanResponse | null>(null);
    const [isLoadingPlans, setIsLoadingPlans] = useState(true);
    const [isPreviewing, setIsPreviewing] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchPlans = async () => {
            try {
                const res = await pricingPlanApi.list();
                // Filter out current plan and inactive plans
                setPlans(res.data.filter(p => p.id !== currentPlanId && p.active));
            } catch (err) {
                console.error("Failed to fetch plans:", err);
            } finally {
                setIsLoadingPlans(false);
            }
        };
        fetchPlans();
    }, [currentPlanId]);

    const handlePlanChange = async (planId: number) => {
        setSelectedPlanId(planId);
        setPreview(null);
        setError(null);

        setIsPreviewing(true);
        try {
            const res = await platformSubscriptionApi.upgradePreview(subscriptionId, planId);
            setPreview(res.data);
        } catch (err) {
            setError(extractApiError(err, "Failed to fetch upgrade preview"));
        } finally {
            setIsPreviewing(false);
        }
    };

    const handleConfirm = async () => {
        if (!selectedPlanId) return;
        setIsSubmitting(true);
        setError(null);
        try {
            await platformSubscriptionApi.upgrade(subscriptionId, { newPlanId: selectedPlanId, notes: "User initiated upgrade" });
            onSuccess();
            onClose();
        } catch (err) {
            setError(extractApiError(err, "Failed to upgrade plan"));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-8 w-full max-w-lg">
                <h2 className="text-xl font-bold mb-2">Upgrade Subscription</h2>
                <p className="text-sm text-gray-500 mb-6">School: {schoolName}</p>

                {error && (
                    <div className="bg-red-50 text-red-600 p-4 rounded-md mb-6 border border-red-200 text-sm">
                        {error}
                    </div>
                )}

                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Select New Plan</label>
                        <select
                            className="w-full border rounded-md p-2"
                            value={selectedPlanId}
                            onChange={(e) => handlePlanChange(parseInt(e.target.value))}
                            disabled={isLoadingPlans || isSubmitting}
                        >
                            <option value="">-- Choose a Plan --</option>
                            {plans.map(p => (
                                <option key={p.id} value={p.id}>
                                    {p.name} (Cap: {p.studentCap}, ₹{p.yearlyPrice.toLocaleString()}/yr)
                                </option>
                            ))}
                        </select>
                    </div>

                    {isPreviewing && (
                        <div className="text-center py-4 text-gray-500 text-sm italic">
                            Calculating prorated amount...
                        </div>
                    )}

                    {preview && (
                        <div className="bg-blue-50 p-4 rounded-lg space-y-2 border border-blue-100">
                            <div className="flex justify-between text-sm">
                                <span className="text-gray-600">Old Plan:</span>
                                <span className="font-medium">{preview.subscription.pricingPlanName}</span>
                            </div>
                            <div className="flex justify-between text-sm">
                                <span className="text-gray-600">Upgrade Amount:</span>
                                <span className="font-bold text-blue-700">₹{preview.proratedAmount.toLocaleString()}</span>
                            </div>
                            <p className="text-[10px] text-gray-400 mt-2 italic">
                                * Prorated for the remaining days of your current cycle. Expiry date remains unchanged.
                            </p>
                        </div>
                    )}

                    <div className="flex justify-end gap-3 mt-6 border-t pt-6">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 border rounded-md hover:bg-gray-50 bg-white"
                            disabled={isSubmitting}
                        >
                            Cancel
                        </button>
                        <button
                            type="button"
                            onClick={handleConfirm}
                            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                            disabled={isSubmitting || !preview}
                        >
                            {isSubmitting ? "Processing..." : "Confirm Upgrade"}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
