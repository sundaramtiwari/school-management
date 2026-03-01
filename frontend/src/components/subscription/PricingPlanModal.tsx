"use client";

import React, { useState } from "react";
import { PricingPlanDto } from "@/lib/subscriptionApi";

interface PricingPlanModalProps {
    plan?: PricingPlanDto | null;
    onClose: () => void;
    onSave: (payload: Partial<PricingPlanDto>) => Promise<void>;
}

export default function PricingPlanModal({ plan, onClose, onSave }: PricingPlanModalProps) {
    const [formData, setFormData] = useState<Partial<PricingPlanDto>>(
        plan || {
            name: "",
            description: "",
            yearlyPrice: 0,
            studentCap: 0,
            trialDaysDefault: 30,
            gracePeriodDaysDefault: 7,
            warningThresholdPercent: 80,
            criticalThresholdPercent: 90,
            active: true,
        }
    );
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsSubmitting(true);
        setError(null);
        try {
            await onSave(formData);
            onClose();
        } catch (err: any) {
            setError(err.response?.data?.message || "Failed to save pricing plan");
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-8 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
                <h2 className="text-2xl font-bold mb-6">{plan ? "Edit Plan" : "Create New Plan"}</h2>

                {error && (
                    <div className="bg-red-50 text-red-600 p-4 rounded-md mb-6 border border-red-200 text-sm">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="grid grid-cols-2 gap-6">
                    <div className="col-span-2">
                        <label className="block text-sm font-medium text-gray-700 mb-1">Plan Name</label>
                        <input
                            type="text"
                            className="w-full border rounded-md p-2"
                            value={formData.name}
                            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                            required
                        />
                    </div>
                    <div className="col-span-2">
                        <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                        <textarea
                            className="w-full border rounded-md p-2"
                            rows={3}
                            value={formData.description}
                            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Yearly Price</label>
                        <input
                            type="number"
                            className="w-full border rounded-md p-2"
                            value={formData.yearlyPrice}
                            onChange={(e) => setFormData({ ...formData, yearlyPrice: parseFloat(e.target.value) })}
                            required
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Student Cap</label>
                        <input
                            type="number"
                            className="w-full border rounded-md p-2"
                            value={formData.studentCap}
                            onChange={(e) => setFormData({ ...formData, studentCap: parseInt(e.target.value) })}
                            required
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Default Trial Days</label>
                        <input
                            type="number"
                            className="w-full border rounded-md p-2"
                            value={formData.trialDaysDefault}
                            onChange={(e) => setFormData({ ...formData, trialDaysDefault: parseInt(e.target.value) })}
                            required
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Default Grace Period (Days)</label>
                        <input
                            type="number"
                            className="w-full border rounded-md p-2"
                            value={formData.gracePeriodDaysDefault}
                            onChange={(e) => setFormData({ ...formData, gracePeriodDaysDefault: parseInt(e.target.value) })}
                            required
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Warning Threshold (%)</label>
                        <input
                            type="number"
                            className="w-full border rounded-md p-2"
                            value={formData.warningThresholdPercent}
                            onChange={(e) => setFormData({ ...formData, warningThresholdPercent: parseInt(e.target.value) })}
                            required
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Critical Threshold (%)</label>
                        <input
                            type="number"
                            className="w-full border rounded-md p-2"
                            value={formData.criticalThresholdPercent}
                            onChange={(e) => setFormData({ ...formData, criticalThresholdPercent: parseInt(e.target.value) })}
                            required
                        />
                    </div>
                    <div className="col-span-2 flex items-center">
                        <input
                            type="checkbox"
                            id="active"
                            className="mr-2 h-4 w-4"
                            checked={formData.active}
                            onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                        />
                        <label htmlFor="active" className="text-sm font-medium text-gray-700">Active</label>
                    </div>

                    <div className="col-span-2 flex justify-end gap-3 mt-6 border-t pt-6">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 border rounded-md hover:bg-gray-50 bg-white"
                            disabled={isSubmitting}
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? "Saving..." : "Save Plan"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
