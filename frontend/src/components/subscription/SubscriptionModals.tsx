"use client";

import React, { useState, useEffect } from "react";
import Modal from "@/components/ui/Modal";
import { pricingPlanApi, platformSubscriptionApi, PricingPlanDto, UpgradePlanResponse } from "@/lib/subscriptionApi";
import { extractApiError } from "@/lib/api";

type ModalProps = {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    schoolId?: number;
    schoolName?: string;
    subscriptionId?: number;
};

/* -------------------------------------------------------------------------- */
/*                                START TRIAL                                 */
/* -------------------------------------------------------------------------- */
export function StartTrialModal({ isOpen, onClose, onSuccess, schoolId, schoolName }: ModalProps) {
    const [plans, setPlans] = useState<PricingPlanDto[]>([]);
    const [selectedPlan, setSelectedPlan] = useState<number>(0);
    const [trialDays, setTrialDays] = useState<number>(30);
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        if (isOpen) {
            pricingPlanApi.list().then(res => {
                const activePlans = res.data.filter(p => p.active);
                setPlans(activePlans);
                if (activePlans.length > 0) setSelectedPlan(activePlans[0].id);
            });
        }
    }, [isOpen]);

    const handleStart = async () => {
        setIsSaving(true);
        try {
            await platformSubscriptionApi.createTrial({ schoolId, pricingPlanId: selectedPlan, trialDays });
            onSuccess();
            onClose();
        } catch (e) {
            alert(extractApiError(e, "Failed to start trial"));
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={`Start Trial: ${schoolName}`}>
            <div className="space-y-4">
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Select Plan</label>
                    <select
                        value={selectedPlan}
                        onChange={(e) => setSelectedPlan(Number(e.target.value))}
                        className="w-full border rounded-lg p-2"
                    >
                        {plans.map(p => <option key={p.id} value={p.id}>{p.name} ({p.studentCap} Students)</option>)}
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Trial Duration (Days)</label>
                    <input
                        type="number"
                        value={trialDays}
                        onChange={(e) => setTrialDays(Number(e.target.value))}
                        className="w-full border rounded-lg p-2"
                    />
                </div>
                <button
                    onClick={handleStart}
                    disabled={isSaving}
                    className="w-full bg-blue-600 text-white py-2.5 rounded-lg font-bold hover:bg-blue-700 disabled:opacity-50"
                >
                    {isSaving ? "Provisioning..." : "Start Trial Now"}
                </button>
            </div>
        </Modal>
    );
}

/* -------------------------------------------------------------------------- */
/*                                  ACTIVATE                                  */
/* -------------------------------------------------------------------------- */
export function ActivateModal({ isOpen, onClose, onSuccess, subscriptionId, schoolName }: ModalProps) {
    const [ref, setRef] = useState("");
    const [date, setDate] = useState(new Date().toISOString().split("T")[0]);
    const [notes, setNotes] = useState("Platform activation");
    const [isSaving, setIsSaving] = useState(false);

    const handleActivate = async () => {
        if (!subscriptionId) return;
        setIsSaving(true);
        try {
            await platformSubscriptionApi.activate(subscriptionId, {
                referenceNumber: ref,
                paymentDate: date,
                notes
            });
            onSuccess();
            onClose();
        } catch (e) {
            alert(extractApiError(e, "Activation failed"));
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={`Activate Subscription: ${schoolName}`}>
            <div className="space-y-4">
                <p className="text-sm text-gray-500 italic">This will move the subscription from TRIAL to ACTIVE status.</p>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Payment Reference</label>
                    <input value={ref} onChange={e => setRef(e.target.value)} placeholder="TXN-123..." className="w-full border rounded-lg p-2" />
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Payment Date</label>
                    <input type="date" value={date} onChange={e => setDate(e.target.value)} className="w-full border rounded-lg p-2" />
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Internal Notes</label>
                    <textarea value={notes} onChange={e => setNotes(e.target.value)} className="w-full border rounded-lg p-2 h-20" />
                </div>
                <button
                    onClick={handleActivate}
                    disabled={isSaving || !ref}
                    className="w-full bg-green-600 text-white py-2.5 rounded-lg font-bold hover:bg-green-700 disabled:opacity-50"
                >
                    {isSaving ? "Activating..." : "Confirm Activation"}
                </button>
            </div>
        </Modal>
    );
}

/* -------------------------------------------------------------------------- */
/*                               RECORD PAYMENT                               */
/* -------------------------------------------------------------------------- */
export function RecordPaymentModal({ isOpen, onClose, onSuccess, subscriptionId, schoolName }: ModalProps) {
    const [amount, setAmount] = useState<number>(0);
    const [ref, setRef] = useState("");
    const [date, setDate] = useState(new Date().toISOString().split("T")[0]);
    const [type, setType] = useState("PAYMENT");
    const [notes, setNotes] = useState("");
    const [isSaving, setIsSaving] = useState(false);

    const handleSave = async () => {
        if (!subscriptionId) return;
        setIsSaving(true);
        try {
            await platformSubscriptionApi.recordPayment(subscriptionId, {
                amount,
                type,
                referenceNumber: ref,
                paymentDate: date,
                notes
            });
            onSuccess();
            onClose();
        } catch (e) {
            alert(extractApiError(e, "Payment recording failed"));
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={`Record Payment: ${schoolName}`}>
            <div className="space-y-4">
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Amount</label>
                    <input type="number" value={amount} onChange={e => setAmount(Number(e.target.value))} className="w-full border rounded-lg p-2" />
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
                    <select value={type} onChange={e => setType(e.target.value)} className="w-full border rounded-lg p-2">
                        <option value="PAYMENT">Regular Payment</option>
                        <option value="UPGRADE_PRORATION">Upgrade Proration</option>
                    </select>
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Reference</label>
                    <input value={ref} onChange={e => setRef(e.target.value)} placeholder="Ref/Chq Number" className="w-full border rounded-lg p-2" />
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Date</label>
                    <input type="date" value={date} onChange={e => setDate(e.target.value)} className="w-full border rounded-lg p-2" />
                </div>
                <button
                    onClick={handleSave}
                    disabled={isSaving || amount <= 0 || !ref}
                    className="w-full bg-blue-600 text-white py-2.5 rounded-lg font-bold hover:bg-blue-700 disabled:opacity-50"
                >
                    {isSaving ? "Recording..." : "Record Payment"}
                </button>
            </div>
        </Modal>
    );
}

/* -------------------------------------------------------------------------- */
/*                                EXTEND DATES                                */
/* -------------------------------------------------------------------------- */
export function ExtendModal({ isOpen, onClose, onSuccess, subscriptionId, schoolName, isTrial }: ModalProps & { isTrial: boolean }) {
    const [days, setDays] = useState(30);
    const [reason, setReason] = useState("");
    const [isSaving, setIsSaving] = useState(false);

    const handleExtend = async () => {
        if (!subscriptionId) return;
        setIsSaving(true);
        try {
            if (isTrial) {
                await platformSubscriptionApi.extendTrial(subscriptionId, { additionalDays: days, reason });
            } else {
                await platformSubscriptionApi.extendSubscription(subscriptionId, { additionalDays: days, reason });
            }
            onSuccess();
            onClose();
        } catch (e) {
            alert(extractApiError(e, "Extension failed"));
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={`Extend ${isTrial ? "Trial" : "Subscription"}: ${schoolName}`}>
            <div className="space-y-4">
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Additional Days</label>
                    <input type="number" value={days} onChange={e => setDays(Number(e.target.value))} className="w-full border rounded-lg p-2" />
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Reason</label>
                    <textarea value={reason} onChange={e => setReason(e.target.value)} className="w-full border rounded-lg p-2 h-20" placeholder="e.g. Requested by Principal" />
                </div>
                <button
                    onClick={handleExtend}
                    disabled={isSaving || !reason}
                    className="w-full bg-orange-600 text-white py-2.5 rounded-lg font-bold hover:bg-orange-700 disabled:opacity-50"
                >
                    {isSaving ? "Processing..." : "Confirm Extension"}
                </button>
            </div>
        </Modal>
    );
}

/* -------------------------------------------------------------------------- */
/*                                   SUSPEND                                  */
/* -------------------------------------------------------------------------- */
export function SuspendModal({ isOpen, onClose, onSuccess, subscriptionId, schoolName }: ModalProps) {
    const [reason, setReason] = useState("");
    const [isSaving, setIsSaving] = useState(false);

    const handleSuspend = async () => {
        if (!subscriptionId) return;
        setIsSaving(true);
        try {
            await platformSubscriptionApi.suspend(subscriptionId, { reason });
            onSuccess();
            onClose();
        } catch (e) {
            alert(extractApiError(e, "Suspension failed"));
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={`Suspend Subscription: ${schoolName}`}>
            <div className="space-y-4">
                <p className="text-sm text-red-600 font-bold bg-red-50 p-3 rounded border border-red-100">
                    WARNING: This will block all student-related operations for this school immediately.
                </p>
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Suspension Reason</label>
                    <textarea value={reason} onChange={e => setReason(e.target.value)} className="w-full border rounded-lg p-2 h-20" />
                </div>
                <button
                    onClick={handleSuspend}
                    disabled={isSaving || !reason}
                    className="w-full bg-red-600 text-white py-2.5 rounded-lg font-bold hover:bg-red-700 disabled:opacity-50"
                >
                    {isSaving ? "Suspending..." : "Confirm Hard Suspension"}
                </button>
            </div>
        </Modal>
    );
}

/* -------------------------------------------------------------------------- */
/*                                 REACTIVATE                                 */
/* -------------------------------------------------------------------------- */
export function ReactivateModal({ isOpen, onClose, onSuccess, subscriptionId, schoolName }: ModalProps) {
    const [reason, setReason] = useState("");
    const [isSaving, setIsSaving] = useState(false);

    const handleReactivate = async () => {
        if (!subscriptionId) return;
        setIsSaving(true);
        try {
            await platformSubscriptionApi.reactivate(subscriptionId, { reason });
            onSuccess();
            onClose();
        } catch (e) {
            alert(extractApiError(e, "Reactivation failed"));
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={`Reactivate Subscription: ${schoolName}`}>
            <div className="space-y-4">
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Reactivation Notes</label>
                    <textarea value={reason} onChange={e => setReason(e.target.value)} className="w-full border rounded-lg p-2 h-20" />
                </div>
                <button
                    onClick={handleReactivate}
                    disabled={isSaving}
                    className="w-full bg-green-600 text-white py-2.5 rounded-lg font-bold hover:bg-green-700 disabled:opacity-50"
                >
                    {isSaving ? "Processing..." : "Confirm Reactivation"}
                </button>
            </div>
        </Modal>
    );
}

/* -------------------------------------------------------------------------- */
/*                            UPGRADE / DOWNGRADE                             */
/* -------------------------------------------------------------------------- */
export function UpgradeDowngradeModal({ isOpen, onClose, onSuccess, subscriptionId, schoolName, isUpgrade }: ModalProps & { isUpgrade: boolean }) {
    const [plans, setPlans] = useState<PricingPlanDto[]>([]);
    const [selectedPlanId, setSelectedPlanId] = useState<number>(0);
    const [preview, setPreview] = useState<UpgradePlanResponse | null>(null);
    const [reason, setReason] = useState("");
    const [isSaving, setIsSaving] = useState(false);
    const [isPreviewLoading, setIsPreviewLoading] = useState(false);

    useEffect(() => {
        if (isOpen) {
            pricingPlanApi.list().then(res => {
                setPlans(res.data.filter(p => p.active));
            });
        }
    }, [isOpen]);

    const handlePreview = async (planId: number) => {
        setSelectedPlanId(planId);
        if (!subscriptionId) return;
        if (!isUpgrade) return; // Only upgrade has preview in BE currently

        setIsPreviewLoading(true);
        try {
            const res = await platformSubscriptionApi.upgradePreview(subscriptionId, planId);
            setPreview(res.data);
        } catch (e) {
            console.error("Preview failed", e);
            setPreview(null);
        } finally {
            setIsPreviewLoading(false);
        }
    };

    const handleConfirm = async () => {
        if (!subscriptionId) return;
        setIsSaving(true);
        try {
            if (isUpgrade) {
                await platformSubscriptionApi.upgrade(subscriptionId, { newPlanId: selectedPlanId, notes: reason });
            } else {
                await platformSubscriptionApi.downgrade(subscriptionId, { newPlanId: selectedPlanId, reason });
            }
            onSuccess();
            onClose();
        } catch (e) {
            alert(extractApiError(e, "Change failed"));
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={`${isUpgrade ? "Upgrade" : "Downgrade"} Plan: ${schoolName}`}>
            <div className="space-y-4">
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">New Plan</label>
                    <select
                        value={selectedPlanId}
                        onChange={(e) => handlePreview(Number(e.target.value))}
                        className="w-full border rounded-lg p-2"
                    >
                        <option value={0}>Select a plan...</option>
                        {plans.map(p => <option key={p.id} value={p.id}>{p.name} ({p.studentCap} Cap)</option>)}
                    </select>
                </div>

                {isPreviewLoading && <div className="text-xs text-blue-600 animate-pulse">Calculating proration...</div>}

                {preview && isUpgrade && (
                    <div className="bg-blue-50 p-4 rounded-lg border border-blue-100 text-sm">
                        <div className="font-bold text-blue-800 mb-1">Proration Summary</div>
                        <div className="flex justify-between">
                            <span>Prorated Amount Due:</span>
                            <span className="font-bold">₹{preview.proratedAmount}</span>
                        </div>
                    </div>
                )}

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Internal Notes / Reason</label>
                    <textarea value={reason} onChange={e => setReason(e.target.value)} className="w-full border rounded-lg p-2 h-20" />
                </div>

                <button
                    onClick={handleConfirm}
                    disabled={isSaving || !selectedPlanId}
                    className={`w-full text-white py-2.5 rounded-lg font-bold disabled:opacity-50 ${isUpgrade ? "bg-indigo-600 hover:bg-indigo-700" : "bg-purple-600 hover:bg-purple-700"}`}
                >
                    {isSaving ? "Executing..." : `Confirm ${isUpgrade ? "Upgrade" : "Downgrade"}`}
                </button>
            </div>
        </Modal>
    );
}
