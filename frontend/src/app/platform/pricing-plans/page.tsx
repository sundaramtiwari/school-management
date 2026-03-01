"use client";

import React, { useEffect, useState } from "react";
import { pricingPlanApi, PricingPlanDto } from "@/lib/subscriptionApi";
import PricingPlanModal from "@/components/subscription/PricingPlanModal";
import { extractApiError } from "@/lib/api";

export default function PricingPlansPage() {
    const [plans, setPlans] = useState<PricingPlanDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingPlan, setEditingPlan] = useState<PricingPlanDto | null>(null);

    const fetchPlans = async () => {
        setIsLoading(true);
        try {
            const response = await pricingPlanApi.list();
            setPlans(response.data);
        } catch (error) {
            console.error("Failed to fetch plans:", error);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchPlans();
    }, []);

    const handleSave = async (payload: Partial<PricingPlanDto>) => {
        if (editingPlan) {
            await pricingPlanApi.update(editingPlan.id, payload);
        } else {
            await pricingPlanApi.create(payload);
        }
        fetchPlans();
    };

    const handleToggleActive = async (plan: PricingPlanDto) => {
        try {
            if (plan.active) {
                await pricingPlanApi.deactivate(plan.id);
            } else {
                await pricingPlanApi.update(plan.id, { ...plan, active: true });
            }
            fetchPlans();
        } catch (error) {
            alert(extractApiError(error, "Failed to update plan status"));
        }
    };

    if (isLoading) {
        return <div className="p-8">Loading plans...</div>;
    }

    return (
        <div className="p-8">
            <div className="flex justify-between items-center mb-8">
                <h1 className="text-2xl font-bold text-gray-800">Pricing Plans</h1>
                <button
                    onClick={() => {
                        setEditingPlan(null);
                        setIsModalOpen(true);
                    }}
                    className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition"
                >
                    Create New Plan
                </button>
            </div>

            <div className="bg-white shadow-sm rounded-lg overflow-hidden border">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Price (Yearly)</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Cap</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Trial/Grace</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Thresholds</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {plans.length === 0 ? (
                            <tr>
                                <td colSpan={7} className="px-6 py-10 text-center text-gray-500">
                                    No pricing plans found. Create one to get started.
                                </td>
                            </tr>
                        ) : (
                            plans.map((plan) => (
                                <tr key={plan.id}>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="text-sm font-medium text-gray-900">{plan.name}</div>
                                        <div className="text-xs text-gray-500 truncate max-w-[200px]">{plan.description}</div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        ₹{plan.yearlyPrice.toLocaleString()}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {plan.studentCap} Students
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {plan.trialDaysDefault}d / {plan.gracePeriodDaysDefault}d
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {plan.warningThresholdPercent}% / {plan.criticalThresholdPercent}%
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${plan.active ? "bg-green-100 text-green-800" : "bg-gray-100 text-gray-800"
                                            }`}>
                                            {plan.active ? "Active" : "Inactive"}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                        <button
                                            onClick={() => {
                                                setEditingPlan(plan);
                                                setIsModalOpen(true);
                                            }}
                                            className="text-blue-600 hover:text-blue-900 mr-4"
                                        >
                                            Edit
                                        </button>
                                        <button
                                            onClick={() => handleToggleActive(plan)}
                                            className={`${plan.active ? "text-red-600 hover:text-red-900" : "text-green-600 hover:text-green-900"}`}
                                        >
                                            {plan.active ? "Deactivate" : "Activate"}
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {isModalOpen && (
                <PricingPlanModal
                    plan={editingPlan}
                    onClose={() => setIsModalOpen(false)}
                    onSave={handleSave}
                />
            )}
        </div>
    );
}
