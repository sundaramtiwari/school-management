"use client";

import { useState } from "react";
import axios from "axios";
import { studentApi } from "@/lib/studentApi";
import { getErrorMessage } from "@/lib/error";
import Modal from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";

/* ---- Types ---- */

interface WithdrawalResponse {
    enrollmentClosed: boolean;
    futureAssignmentsDeactivated: number;
    futureAssignmentsSkippedDueToPayment: number;
    skippedAssignmentIds: number[];
    transportUnenrolled: boolean;
}

interface Props {
    studentId: number;
    studentName: string;
    sessionId: number;
    isOpen: boolean;
    onClose: () => void;
    onSuccess: (response: WithdrawalResponse) => void;
}

/* ---- Helpers ---- */

function todayISO(): string {
    return new Date().toISOString().split("T")[0];
}

/* ---- Component ---- */

export default function WithdrawStudentModal({
    studentId,
    studentName,
    sessionId,
    isOpen,
    onClose,
    onSuccess,
}: Props) {
    const { showToast } = useToast();

    const [withdrawalDate, setWithdrawalDate] = useState<string>(todayISO());
    const [reason, setReason] = useState<string>("");
    const [confirmed, setConfirmed] = useState<boolean>(false);
    const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

    function reset() {
        setWithdrawalDate(todayISO());
        setReason("");
        setConfirmed(false);
        setIsSubmitting(false);
    }

    function handleClose() {
        if (isSubmitting) return; // prevent close mid-flight
        reset();
        onClose();
    }

    async function handleSubmit() {
        if (!withdrawalDate || !confirmed || isSubmitting) return;

        try {
            setIsSubmitting(true);

            const res = await studentApi.withdraw(studentId, {
                sessionId,
                withdrawalDate,
                reason: reason.trim() || undefined,
            });

            const data: WithdrawalResponse = res.data;
            onSuccess(data);
            reset();
        } catch (err: unknown) {
            if (axios.isAxiosError(err)) {
                const status = err.response?.status;

                if (status === 404) {
                    showToast("Active enrollment not found for current session.", "error");
                } else if (status === 400) {
                    showToast(getErrorMessage(err), "error");
                } else {
                    showToast("Withdrawal failed. Please try again.", "error");
                }
            } else {
                showToast("Withdrawal failed. Please try again.", "error");
            }
        } finally {
            setIsSubmitting(false);
        }
    }

    const canConfirm = !!withdrawalDate && confirmed && !isSubmitting;

    return (
        <Modal
            isOpen={isOpen}
            onClose={handleClose}
            title="Withdraw Student"
            maxWidth="max-w-lg"
            bodyClassName="px-6 py-5"
            footer={
                <div className="flex gap-2 justify-end">
                    <button
                        onClick={handleClose}
                        disabled={isSubmitting}
                        className="px-5 py-2 rounded-md bg-white border border-gray-300 font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-50"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleSubmit}
                        disabled={!canConfirm}
                        className="px-6 py-2 rounded-md bg-red-600 text-white font-medium hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 min-w-[120px] justify-center"
                    >
                        {isSubmitting ? (
                            <>
                                {/* Spinner */}
                                <svg
                                    className="animate-spin h-4 w-4 text-white"
                                    xmlns="http://www.w3.org/2000/svg"
                                    fill="none"
                                    viewBox="0 0 24 24"
                                >
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                    <path
                                        className="opacity-75"
                                        fill="currentColor"
                                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                                    />
                                </svg>
                                <span>Withdrawing…</span>
                            </>
                        ) : (
                            "Confirm Withdrawal"
                        )}
                    </button>
                </div>
            }
        >
            <div className="space-y-5">
                {/* Student label */}
                <p className="text-sm text-gray-600">
                    You are withdrawing{" "}
                    <span className="font-semibold text-gray-900">{studentName}</span>{" "}
                    from the current session.
                </p>

                {/* Warning banner */}
                <div className="flex gap-3 rounded-md border border-amber-300 bg-amber-50 px-4 py-3">
                    <span className="mt-0.5 text-amber-500 text-lg leading-none">⚠</span>
                    <p className="text-sm text-amber-800 leading-relaxed">
                        <span className="font-semibold block mb-0.5">Financial impact</span>
                        This will close enrollment and deactivate all unpaid future fee
                        assignments. <strong>This action cannot be undone automatically.</strong>
                    </p>
                </div>

                {/* Withdrawal Date */}
                <div className="space-y-1">
                    <label className="block text-xs font-bold text-gray-500 uppercase tracking-wide">
                        Withdrawal Date <span className="text-red-500">*</span>
                    </label>
                    <input
                        type="date"
                        value={withdrawalDate}
                        onChange={(e) => setWithdrawalDate(e.target.value)}
                        disabled={isSubmitting}
                        className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-red-400 focus:border-red-400 disabled:bg-gray-50"
                    />
                </div>

                {/* Reason */}
                <div className="space-y-1">
                    <label className="block text-xs font-bold text-gray-500 uppercase tracking-wide">
                        Reason{" "}
                        <span className="normal-case font-normal text-gray-400">(optional)</span>
                    </label>
                    <textarea
                        rows={3}
                        value={reason}
                        onChange={(e) => setReason(e.target.value)}
                        disabled={isSubmitting}
                        placeholder="e.g. Family relocated, transferred to another school…"
                        className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-red-400 focus:border-red-400 resize-none disabled:bg-gray-50"
                    />
                </div>

                {/* Confirmation checkbox */}
                <label className="flex items-start gap-2.5 cursor-pointer select-none">
                    <input
                        type="checkbox"
                        checked={confirmed}
                        onChange={(e) => setConfirmed(e.target.checked)}
                        disabled={isSubmitting}
                        className="mt-0.5 h-4 w-4 accent-red-600 cursor-pointer"
                    />
                    <span className="text-sm text-gray-700 leading-snug">
                        I understand this modifies financial records.
                    </span>
                </label>
            </div>
        </Modal>
    );
}
