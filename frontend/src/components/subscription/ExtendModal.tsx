"use client";

import React, { useState } from "react";

interface ExtendModalProps {
    subscriptionId: number;
    schoolName: string;
    type: "TRIAL" | "SUBSCRIPTION";
    onClose: () => void;
    onSave: (payload: { additionalDays: number; reason: string }) => Promise<void>;
}

export default function ExtendModal({ subscriptionId, schoolName, type, onClose, onSave }: ExtendModalProps) {
    const [formData, setFormData] = useState({
        additionalDays: 30,
        reason: ""
    });
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
            setError(err.response?.data?.message || `Failed to extend ${type.toLowerCase()}`);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-8 w-full max-w-md">
                <h2 className="text-xl font-bold mb-2">Extend {type === "TRIAL" ? "Trial" : "Subscription"}</h2>
                <p className="text-sm text-gray-500 mb-6">School: {schoolName}</p>

                {error && (
                    <div className="bg-red-50 text-red-600 p-4 rounded-md mb-6 border border-red-200 text-sm">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Additional Days</label>
                        <input
                            type="number"
                            className="w-full border rounded-md p-2"
                            value={formData.additionalDays}
                            onChange={(e) => setFormData({ ...formData, additionalDays: parseInt(e.target.value) })}
                            required
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Reason</label>
                        <textarea
                            className="w-full border rounded-md p-2"
                            rows={3}
                            placeholder="Reason for extension..."
                            value={formData.reason}
                            onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
                            required
                        />
                    </div>

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
                            type="submit"
                            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? "Extending..." : "Confirm Extension"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
