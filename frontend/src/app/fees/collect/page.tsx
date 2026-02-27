"use client";

import React, { useCallback, useEffect, useState, type ChangeEvent, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { api } from "@/lib/api";
import { canCollectFees, canManageFees } from "@/lib/permissions";
import { useToast } from "@/components/ui/Toast";
import { useSession } from "@/context/SessionContext";
import { useAuth } from "@/context/AuthContext";
import Modal from "@/components/ui/Modal";

type SchoolClass = { id: number; name: string; section: string; sessionId: number };
type Student = { id: number; firstName: string; lastName: string; admissionNumber: string };
type FeeSummary = {
    totalFee: number;
    totalDiscount: number;
    totalFunding: number;
    totalPaid: number;
    pendingFee: number;
    totalLateFeeAccrued: number;
    totalLateFeePaid: number;
    totalLateFeeWaived: number;
    status: string
};
type Payment = { id: number; amountPaid: number; paymentDate: string; mode: string; remarks: string };
type FeeAssignment = {
    id: number;
    feeTypeName: string;
    amount: number;
    frequency?: string;
    periodsPerYear?: number;
    periodsElapsed?: number;
    amountPerPeriod?: number;
    annualAmount?: number;
    dueTillDate?: number;
    nextDueDate?: string | null;
    pendingTillDate?: number;
    remainingForSession?: number;
    totalDiscountAmount: number;
    sponsorCoveredAmount: number;
    principalPaid: number;
    lateFeeAccrued: number;
    lateFeePaid: number;
    lateFeeWaived: number;
    status: string;
};

type FeeAdjustment = {
    id: number;
    type: string;
    discountType?: string;
    discountName?: string;
    amount: number;
    remarks?: string;
    createdByName?: string;
    createdAt: string;
};

type DiscountDefinition = {
    id: number;
    name: string;
    type: string;
    amountValue: number;
    active: boolean;
};

function extractApiErrorMessage(error: unknown): string | null {
    if (
        typeof error === "object"
        && error !== null
        && "response" in error
        && typeof (error as { response?: unknown }).response === "object"
        && (error as { response?: unknown }).response !== null
        && "data" in ((error as { response: { data?: unknown } }).response)
        && typeof ((error as { response: { data?: unknown } }).response.data) === "object"
        && ((error as { response: { data?: unknown } }).response.data) !== null
        && "message" in (((error as { response: { data: { message?: unknown } } }).response.data))
        && typeof ((error as { response: { data: { message?: unknown } } }).response.data.message) === "string"
    ) {
        return (error as { response: { data: { message: string } } }).response.data.message;
    }
    return null;
}

export default function FeeCollectPage() {
    return (
        <Suspense fallback={<div className="p-12 text-center text-gray-400">Loading Billing...</div>}>
            <FeeCollectContent />
        </Suspense>
    );
}

function FeeCollectContent() {
    const { user } = useAuth();
    const { showToast } = useToast();
    const { currentSession } = useSession();
    const searchParams = useSearchParams();

    const canUserCollectFees = canCollectFees(user?.role);
    const canUserManageFees = canManageFees(user?.role);

    /* -------- State -------- */
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [selectedClass, setSelectedClass] = useState<number | "">("");

    const [students, setStudents] = useState<Student[]>([]);
    const [selectedStudent, setSelectedStudent] = useState<number | "">("");

    const [summary, setSummary] = useState<FeeSummary | null>(null);
    const [history, setHistory] = useState<Payment[]>([]);
    const [breakdown, setBreakdown] = useState<FeeAssignment[]>([]);

    const [expandedAssignmentId, setExpandedAssignmentId] = useState<number | null>(null);
    const [activeActionDropdown, setActiveActionDropdown] = useState<number | null>(null);
    const [adjustmentHistory, setAdjustmentHistory] = useState<Record<number, FeeAdjustment[]>>({});
    const [loadingAdjustments, setLoadingAdjustments] = useState<Record<number, boolean>>({});

    const [discounts, setDiscounts] = useState<DiscountDefinition[]>([]);
    const [showDiscountModal, setShowDiscountModal] = useState(false);
    const [selectedAssignmentForDiscount, setSelectedAssignmentForDiscount] = useState<number | null>(null);
    const [discountForm, setDiscountForm] = useState({ discountId: "", remarks: "" });
    const [isApplyingDiscount, setIsApplyingDiscount] = useState(false);
    const [showWaiverModal, setShowWaiverModal] = useState(false);
    const [selectedAssignmentForWaiver, setSelectedAssignmentForWaiver] = useState<number | null>(null);
    const [selectedAssignmentPendingLateFee, setSelectedAssignmentPendingLateFee] = useState(0);
    const [waiverForm, setWaiverForm] = useState({ amount: "", remarks: "" });
    const [isApplyingWaiver, setIsApplyingWaiver] = useState(false);
    const [allocations, setAllocations] = useState<Record<number, { principal: string; payLateFee: boolean }>>({});

    const [loading, setLoading] = useState(false);
    const [loadingStudents, setLoadingStudents] = useState(false);
    const [isProcessing, setIsProcessing] = useState(false);
    const [isDownloading, setIsDownloading] = useState(false);

    const [paymentMode, setPaymentMode] = useState("CASH");
    const [remarks, setRemarks] = useState("");

    /* -------- Initial Load -------- */
    const loadClasses = useCallback(async () => {
        try {
            const res = await api.get("/api/classes/mine?size=100");
            setClasses(res.data.content || []);
        } catch {
            showToast("Failed to initialize billing classes", "error");
        }
    }, [showToast]);

    const loadDiscounts = useCallback(async () => {
        try {
            const res = await api.get("/api/fees/discount-definitions");
            setDiscounts(res.data || []);
        } catch {
            // fail silently for discounts, will handle empty state in modal
        }
    }, []);

    useEffect(() => {
        void loadClasses();
        if (canCollectFees(user?.role) || canManageFees(user?.role)) {
            void loadDiscounts();
        }
    }, [currentSession, loadClasses, loadDiscounts]);

    // Handle deep-linking from query params
    useEffect(() => {
        const studentId = searchParams.get("student");
        if (studentId && currentSession) {
            void (async () => {
                try {
                    // 1. Fetch Student Details to get their class
                    const stdRes = await api.get(`/api/students/${studentId}`);
                    const student = stdRes.data;

                    if (student.currentClassId) {
                        setSelectedClass(student.currentClassId);

                        // 2. Load all students for that class to populate dropdown
                        setLoadingStudents(true);
                        const studentsRes = await api.get(`/api/students/by-class/${student.currentClassId}?size=100`);
                        setStudents(studentsRes.data.content || []);
                        setLoadingStudents(false);

                        // 3. Set selected student and load their ledger
                        setSelectedStudent(Number(studentId));
                        void loadStudentData(Number(studentId));
                    }
                } catch (err) {
                    console.error("Deep-linking failed", err);
                    showToast("Failed to auto-load student from link", "error");
                }
            })();
        }
    }, [searchParams, currentSession, showToast]);

    /* -------- Handlers -------- */
    async function onClassChange(e: ChangeEvent<HTMLSelectElement>) {
        const clsId = e.target.value ? Number(e.target.value) : "";
        setSelectedClass(clsId);
        setStudents([]);
        setSelectedStudent("");
        setSummary(null);
        setHistory([]);

        if (clsId) {
            try {
                setLoadingStudents(true);
                const res = await api.get(`/api/students/by-class/${clsId}?size=100`);
                setStudents(res.data.content || []);
            } catch {
                showToast("Failed to pull student ledger", "error");
            } finally {
                setLoadingStudents(false);
            }
        }
    }

    async function onStudentChange(e: ChangeEvent<HTMLSelectElement>) {
        const stdId = e.target.value ? Number(e.target.value) : "";
        setSelectedStudent(stdId);
        setSummary(null);
        setHistory([]);
        if (stdId) void loadStudentData(stdId);
    }

    async function loadStudentData(stdId: number) {
        if (!currentSession) return;
        try {
            setLoading(true);
            const [sumRes, histRes, breakRes] = await Promise.all([
                api.get(`/api/fees/summary/students/${stdId}`),
                api.get(`/api/fees/payments/students/${stdId}`),
                api.get(`/api/fees/assignments/students/${stdId}`)
            ]);

            setSummary(sumRes.data);
            setHistory(histRes.data || []);
            setBreakdown(breakRes.data || []);

            // Initialize allocations
            const initialAllocations: Record<number, { principal: string; payLateFee: boolean }> = {};
            (breakRes.data || []).forEach((item: FeeAssignment) => {
                initialAllocations[item.id] = { principal: "", payLateFee: false };
            });
            setAllocations(initialAllocations);
        } catch {
            showToast("Billing synchronization failed", "error");
        } finally {
            setLoading(false);
        }
    }

    const calculateTotalAllocated = useCallback(() => {
        let total = 0;
        Object.keys(allocations).forEach(id => {
            const alloc = allocations[Number(id)];
            const principal = Number(alloc.principal) || 0;
            const item = breakdown.find(b => b.id === Number(id));
            const lateFee = (alloc.payLateFee && item) ? Math.max(0, item.lateFeeAccrued - item.lateFeePaid - item.lateFeeWaived) : 0;
            total += principal + lateFee;
        });
        return total;
    }, [allocations, breakdown]);

    async function makePayment() {
        if (!selectedStudent) return;

        const totalAmount = calculateTotalAllocated();
        if (totalAmount <= 0) {
            showToast("Please allocate some amount to pay", "warning");
            return;
        }

        const paymentAllocations = Object.entries(allocations)
            .filter(([, alloc]) => Number(alloc.principal) > 0 || alloc.payLateFee)
            .map(([id, alloc]) => {
                const item = breakdown.find(b => b.id === Number(id));
                const lateFeeAmount = (alloc.payLateFee && item) ? Math.max(0, item.lateFeeAccrued - item.lateFeePaid - item.lateFeeWaived) : 0;
                return {
                    assignmentId: Number(id),
                    principalAmount: Number(alloc.principal) || 0,
                    lateFeeAmount: lateFeeAmount
                };
            });

        try {
            setIsProcessing(true);
            await api.post("/api/fees/payments", {
                studentId: selectedStudent,
                allocations: paymentAllocations,
                mode: paymentMode,
                remarks,
                paymentDate: new Date().toISOString().split('T')[0]
            });

            showToast("Payment Processed Successfully!", "success");
            setRemarks("");
            setAllocations({});
            loadStudentData(Number(selectedStudent));
        } catch (e: unknown) {
            const message = extractApiErrorMessage(e) || "Processing failed";
            showToast(message, "error");
        } finally {
            setIsProcessing(false);
        }
    }

    async function downloadReceipt(pid: number) {
        try {
            setIsDownloading(true);
            showToast("Generating receipt...", "info");
            const res = await api.get(`/api/fees/payments/${pid}/receipt`, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `receipt_transaction_${pid}.pdf`);
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            showToast("Download complete", "success");
        } catch {
            showToast("Receipt generation failed", "error");
        } finally {
            setIsDownloading(false);
        }
    }

    async function downloadChallan() {
        if (!selectedStudent || !selectedClass || !currentSession) return;
        try {
            setIsDownloading(true);
            showToast("Generating challan...", "info");
            const res = await api.get(`/api/fees/challan/student/${selectedStudent}`, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `fee_challan_${selectedStudent}.pdf`);
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            showToast("Download complete", "success");
        } catch {
            showToast("Challan generation failed", "error");
        } finally {
            setIsDownloading(false);
        }
    }

    async function toggleExpand(id: number) {
        if (expandedAssignmentId === id) {
            setExpandedAssignmentId(null);
            return;
        }

        setExpandedAssignmentId(id);

        if (!adjustmentHistory[id]) {
            try {
                setLoadingAdjustments(prev => ({ ...prev, [id]: true }));
                const res = await api.get(`/api/fees/assignments/${id}/adjustments`);
                setAdjustmentHistory(prev => ({
                    ...prev,
                    [id]: res.data || []
                }));
            } catch {
                showToast("Failed to load adjustment history", "error");
            } finally {
                setLoadingAdjustments(prev => ({ ...prev, [id]: false }));
            }
        }
    }

    async function applyDiscount() {
        if (!selectedAssignmentForDiscount || !discountForm.discountId) {
            showToast("Please select a discount policy", "warning");
            return;
        }

        try {
            setIsApplyingDiscount(true);
            await api.post(`/api/fees/assignments/${selectedAssignmentForDiscount}/discount`, {
                discountDefinitionId: Number(discountForm.discountId),
                remarks: discountForm.remarks.trim()
            });

            showToast("Discount applied successfully!", "success");
            setShowDiscountModal(false);
            setDiscountForm({ discountId: "", remarks: "" });

            if (selectedStudent) {
                void loadStudentData(Number(selectedStudent));
                setAdjustmentHistory(prev => {
                    const newHistory = { ...prev };
                    delete newHistory[selectedAssignmentForDiscount];
                    return newHistory;
                });
            }
        } catch (e: unknown) {
            showToast(extractApiErrorMessage(e) || "Failed to apply discount", "error");
        } finally {
            setIsApplyingDiscount(false);
        }
    }

    async function applyLateFeeWaiver() {
        const assignmentId = selectedAssignmentForWaiver;
        const studentId = selectedStudent ? Number(selectedStudent) : null;
        if (!assignmentId) {
            showToast("No assignment selected for waiver", "warning");
            return;
        }

        const waiverAmount = Number(waiverForm.amount);
        if (!Number.isFinite(waiverAmount) || waiverAmount <= 0) {
            showToast("Enter a valid waiver amount", "warning");
            return;
        }
        if (waiverAmount > selectedAssignmentPendingLateFee) {
            showToast("Waiver amount cannot exceed pending late fee", "warning");
            return;
        }
        if (!waiverForm.remarks.trim()) {
            showToast("Remarks are required for late fee waiver", "warning");
            return;
        }

        try {
            setIsApplyingWaiver(true);
            await api.post(`/api/fees/assignments/${assignmentId}/waive-late-fee`, {
                waiverAmount,
                remarks: waiverForm.remarks.trim()
            });

            setShowWaiverModal(false);
            setWaiverForm({ amount: "", remarks: "" });
            setSelectedAssignmentForWaiver(null);
            setSelectedAssignmentPendingLateFee(0);

            setAdjustmentHistory(prev => {
                const newHistory = { ...prev };
                delete newHistory[assignmentId];
                return newHistory;
            });

            if (studentId) {
                await loadStudentData(studentId);
            }
            showToast("Late fee waived successfully", "success");
        } catch (e: unknown) {
            showToast(extractApiErrorMessage(e) || "Failed to apply late fee waiver", "error");
        } finally {
            setIsApplyingWaiver(false);
        }
    }

    const totalAnnual = breakdown.reduce((sum, item) => sum + (item.annualAmount ?? 0), 0);
    const totalPaid = breakdown.reduce((sum, item) => sum + (item.principalPaid ?? 0), 0);
    const totalAccrued = breakdown.reduce((sum, item) => sum + (item.dueTillDate ?? 0), 0);
    const accruedPending = breakdown.reduce((sum, item) => sum + (item.pendingTillDate ?? 0), 0);
    const remainingForSession = breakdown.reduce((sum, item) => sum + (item.remainingForSession ?? 0), 0);
    const totalAdvance = breakdown.reduce((sum, item) => sum + Math.max(0, (item.principalPaid ?? 0) - (item.dueTillDate ?? 0)), 0);

    return (
        <div className="space-y-6">
            <header>
                <h1 className="text-3xl font-bold text-gray-800">Billing &amp; Collections</h1>
                <p className="text-gray-500 mt-1">Settle student dues and manage transactional records for <span className="text-blue-600 font-bold">{currentSession?.name || "current session"}</span>.</p>
            </header>

            <div className="bg-white p-6 border rounded-2xl shadow-sm flex flex-wrap gap-6 items-end">
                <div className="flex-1 min-w-[250px]">
                    <label className="block text-xs font-bold uppercase text-gray-400 mb-2 ml-1">Class Registry</label>
                    <select className="input-ref" value={selectedClass} onChange={onClassChange}>
                        <option value="">Select Billing Target Class</option>
                        {classes.map(c => <option key={c.id} value={c.id}>{c.name} {c.section}</option>)}
                    </select>
                </div>
                <div className="flex-1 min-w-[250px]">
                    <label className="block text-xs font-bold uppercase text-gray-400 mb-2 ml-1">Student Account</label>
                    <select className="input-ref" value={selectedStudent} onChange={onStudentChange} disabled={!selectedClass || loadingStudents}>
                        <option value="">{loadingStudents ? "Loading roster..." : "Select Student Account"}</option>
                        {students.map(s => <option key={s.id} value={s.id}>{s.firstName} {s.lastName} ({s.admissionNumber})</option>)}
                    </select>
                </div>
            </div>

            {loading ? (
                <div className="bg-white p-12 rounded-2xl border text-center text-gray-400 italic">
                    Syncing financial data with ledger...
                </div>
            ) : (selectedStudent && summary) ? (
                <div className="flex flex-col gap-6">
                    {/* Single 3-col grid: left = controls stacked, right = tables stacked with no gap */}
                    <div className="flex flex-col gap-6">
                        {/* TOP SECTION: Snapshot Card + Post Payment Transaction (side-by-side on wide monitors) */}
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-stretch">
                            {/* Finance Position card */}
                            <div className="bg-gray-900 text-white rounded-2xl p-8 shadow-2xl relative overflow-hidden flex flex-col justify-between">
                                <div className="absolute top-0 right-0 p-4 opacity-10 text-6xl">₹</div>
                                <div>
                                    <h3 className="text-xs font-black uppercase tracking-widest text-gray-400 mb-2">Finance Position</h3>
                                    <p className="text-5xl font-black">₹ {remainingForSession.toLocaleString()}</p>
                                    <p className="text-[10px] text-gray-400 font-bold uppercase mt-1">Total Remaining For Session</p>
                                </div>

                                <div className="mt-6 flex flex-col gap-2 text-[10px] font-bold uppercase tracking-tight border-t border-white/10 pt-4">
                                    <div className="flex justify-between">
                                        <span className="text-gray-400">Total Annual Principal:</span>
                                        <span className="text-white">₹ {totalAnnual.toLocaleString()}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-gray-400">Total Principal Paid:</span>
                                        <span className="text-green-400">₹ {totalPaid.toLocaleString()}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-gray-400">Accrued Till Today:</span>
                                        <span className="text-blue-400">₹ {totalAccrued.toLocaleString()}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-gray-400">Accrued Pending Now:</span>
                                        <span className="text-red-400 font-black">₹ {accruedPending.toLocaleString()}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-gray-400">Advance Paid:</span>
                                        <span className="text-emerald-400">₹ {totalAdvance.toLocaleString()}</span>
                                    </div>
                                    <div className="flex justify-between border-t border-white/5 pt-2 mt-1">
                                        <span className="text-gray-400">Accrued Late Fees (O/S):</span>
                                        <span className="text-orange-400">₹ {((summary.totalLateFeeAccrued ?? 0) - (summary.totalLateFeePaid ?? 0) - (summary.totalLateFeeWaived ?? 0)).toLocaleString()}</span>
                                    </div>
                                </div>

                                <div className="mt-8 pt-6 border-t border-white/10 space-y-4">
                                    <button
                                        onClick={downloadChallan}
                                        disabled={isDownloading}
                                        className="w-full bg-white/10 hover:bg-white/20 text-white py-3 rounded-xl font-bold transition-all flex items-center justify-center gap-2 border border-white/5 disabled:opacity-50"
                                    >
                                        {isDownloading ? "⏳ Generating..." : "📄 Generate Academic Challan"}
                                    </button>
                                </div>
                            </div>

                            {/* Post Payment Transaction form */}
                            <div className="bg-white border rounded-2xl p-8 shadow-sm space-y-5 flex flex-col justify-between">
                                <h3 className="font-black text-gray-800 border-b pb-4 text-xs uppercase tracking-widest">Post Payment Transaction</h3>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-start">
                                    <div className="space-y-4">
                                        <div>
                                            <label className="block text-[10px] font-black uppercase text-gray-400 mb-1 ml-1">Total Collection Amount</label>
                                            <div className="relative">
                                                <span className="absolute left-4 top-1/2 -translate-y-1/2 font-bold text-gray-400">₹</span>
                                                <div className="input-ref pl-10 text-xl font-black bg-gray-50 flex items-center">
                                                    {calculateTotalAllocated().toLocaleString(undefined, { minimumFractionDigits: 2 })}
                                                </div>
                                            </div>
                                        </div>
                                        <div>
                                            <label className="block text-[10px] font-black uppercase text-gray-400 mb-1 ml-1">Payment Channel</label>
                                            <select className="input-ref font-bold" value={paymentMode} onChange={e => setPaymentMode(e.target.value)}>
                                                <option value="CASH">Liquid Cash</option>
                                                <option value="ONLINE">Digital/UPI</option>
                                                <option value="BANK_TRANSFER">Bank Transfer</option>
                                                <option value="CHEQUE">Banker&apos;s Cheque</option>
                                            </select>
                                        </div>
                                    </div>
                                    <div className="space-y-4">
                                        <div>
                                            <label className="block text-[10px] font-black uppercase text-gray-400 mb-1 ml-1">Internal Remarks</label>
                                            <textarea
                                                placeholder="Reference or narration..."
                                                className="input-ref h-[106px] text-sm"
                                                value={remarks}
                                                onChange={e => setRemarks(e.target.value)}
                                            />
                                        </div>
                                    </div>
                                </div>
                                <div className="pt-4 border-t mt-4">
                                    {canUserCollectFees ? (
                                        <button
                                            onClick={makePayment}
                                            disabled={calculateTotalAllocated() <= 0 || isProcessing}
                                            className="w-full bg-green-600 hover:bg-green-700 text-white py-4 rounded-xl font-black shadow-lg shadow-green-200 transition-all disabled:opacity-50 uppercase text-xs tracking-widest"
                                        >
                                            {isProcessing ? "Transacting..." : "Commit Transaction"}
                                        </button>
                                    ) : (
                                        <div className="p-4 bg-red-50 text-red-600 rounded-xl text-xs font-bold text-center border border-red-100">
                                            You do not have permission to collect fees.
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>

                        {/* BOTTOM SECTION: Fee Breakdown + Transaction Audit Log (Stacked beneath) */}
                        <div className="flex flex-col">

                            {/* Fee Breakdown — top half of right column */}
                            <div className="bg-white border border-b-0 rounded-t-2xl overflow-hidden shadow-sm">
                                <div className="p-6 border-b border-gray-100 flex items-center justify-between bg-gray-50/50">
                                    <h3 className="text-xs font-black uppercase tracking-widest text-gray-500">Fee Breakdown</h3>
                                </div>
                                <div className="p-6 overflow-x-auto">
                                    <div className="min-w-fit">
                                        <table className="w-full text-left text-[11px]">
                                            <thead className="text-gray-400 font-bold uppercase border-b">
                                                <tr>
                                                    <th className="pb-2 w-8"></th>
                                                    <th className="pb-2">Type</th>
                                                    <th className="pb-2 text-right">Annual</th>
                                                    <th className="pb-2 text-right">Accrued</th>
                                                    <th className="pb-2 text-right">Paid</th>
                                                    <th className="pb-2 text-right">Advance</th>
                                                    <th className="pb-2 text-right">Pending Now</th>
                                                    <th className="pb-2 text-right">Remaining</th>
                                                    <th className="pb-2 text-center">Status</th>
                                                    <th className="pb-2 text-center">Pay Now</th>
                                                    {canUserManageFees && <th className="pb-2 text-center w-36">Actions</th>}
                                                </tr>
                                            </thead>
                                            <tbody className="divide-y divide-gray-100">
                                                {breakdown.map((item) => {
                                                    const pendingAmount = item.pendingTillDate ?? 0;
                                                    const principalPaid = item.principalPaid ?? 0;
                                                    const dueTillDate = item.dueTillDate ?? 0;
                                                    const remainingAmount = item.remainingForSession ?? 0;
                                                    const advanceAmount = Math.max(0, principalPaid - dueTillDate);

                                                    let accrualStatus: "CLEARED" | "DUE" | "PARTIAL" = "DUE";
                                                    if (pendingAmount > 0) {
                                                        accrualStatus = "DUE";
                                                    } else if (remainingAmount > 0) {
                                                        accrualStatus = "PARTIAL";
                                                    } else {
                                                        accrualStatus = "CLEARED";
                                                    }

                                                    const statusColors = {
                                                        CLEARED: "bg-green-100 text-green-600",
                                                        DUE: "bg-red-100 text-red-600",
                                                        PARTIAL: "bg-amber-100 text-amber-600"
                                                    };

                                                    const outstandingLateFee = Math.max(0, (item.lateFeeAccrued || 0) - (item.lateFeePaid || 0) - (item.lateFeeWaived || 0));
                                                    return (
                                                        <React.Fragment key={item.id}>
                                                            <tr className="hover:bg-gray-50 transition-colors">
                                                                <td className="py-2.5 text-center">
                                                                    <button
                                                                        onClick={() => toggleExpand(item.id)}
                                                                        className="text-gray-400 hover:text-gray-700"
                                                                    >
                                                                        {expandedAssignmentId === item.id ? "▼" : "▶"}
                                                                    </button>
                                                                </td>
                                                                <td className="py-2.5 font-bold text-gray-700">{item.feeTypeName || "Miscellaneous"}</td>
                                                                <td className="py-2.5 text-right">₹ {(item.annualAmount ?? 0).toLocaleString()}</td>
                                                                <td className="py-2.5 text-right font-medium">₹ {dueTillDate.toLocaleString()}</td>
                                                                <td className="py-2.5 text-right text-green-600">₹ {principalPaid.toLocaleString()}</td>
                                                                <td className="py-2.5 text-right">
                                                                    {advanceAmount > 0 ? (
                                                                        <span className="text-emerald-600 font-bold">₹ {advanceAmount.toLocaleString()}</span>
                                                                    ) : (
                                                                        <span className="text-gray-300">—</span>
                                                                    )}
                                                                </td>
                                                                <td className={`py-2.5 text-right font-black ${pendingAmount > 0 ? "text-red-600" : "text-green-600"}`}>₹ {Math.max(0, pendingAmount).toLocaleString()}</td>
                                                                <td className="py-2.5 text-right font-medium text-gray-400">₹ {remainingAmount.toLocaleString()}</td>
                                                                <td className="py-2.5 text-center">
                                                                    <span className={`px-2 py-0.5 rounded-full text-[9px] font-black uppercase ${statusColors[accrualStatus]}`}>
                                                                        {accrualStatus}
                                                                    </span>
                                                                </td>
                                                                <td className="py-2.5 text-center">
                                                                    <div className="flex flex-col gap-1 items-center">
                                                                        <div className="relative">
                                                                            <span className="absolute left-2 top-1/2 -translate-y-1/2 text-[9px] text-gray-400">₹</span>
                                                                            <input
                                                                                type="number"
                                                                                placeholder="Princ."
                                                                                className="w-20 text-[10px] pl-4 pr-1 py-1 border rounded focus:ring-1 focus:ring-blue-500 outline-none"
                                                                                value={allocations[item.id]?.principal || ""}
                                                                                onChange={e => {
                                                                                    const val = e.target.value;
                                                                                    const maxPrincipal = item.remainingForSession ?? 0;
                                                                                    if (Number(val) > maxPrincipal) {
                                                                                        showToast(`Principal payment for ${item.feeTypeName} cannot exceed ₹${maxPrincipal}`, "warning");
                                                                                        return;
                                                                                    }
                                                                                    setAllocations(prev => ({
                                                                                        ...prev,
                                                                                        [item.id]: { ...prev[item.id], principal: val }
                                                                                    }));
                                                                                }}
                                                                            />
                                                                        </div>
                                                                        {outstandingLateFee > 0 && (
                                                                            <label className="flex items-center gap-1 cursor-pointer">
                                                                                <input
                                                                                    type="checkbox"
                                                                                    className="w-3 h-3 text-blue-600 rounded"
                                                                                    checked={allocations[item.id]?.payLateFee || false}
                                                                                    onChange={e => {
                                                                                        setAllocations(prev => ({
                                                                                            ...prev,
                                                                                            [item.id]: { ...prev[item.id], payLateFee: e.target.checked }
                                                                                        }));
                                                                                    }}
                                                                                />
                                                                                <span className="text-[9px] font-bold text-orange-600">Late Fee (₹{outstandingLateFee})</span>
                                                                            </label>
                                                                        )}
                                                                    </div>
                                                                </td>
                                                                {canUserManageFees && (
                                                                    <td className="py-2.5 text-center">
                                                                        <div className="relative inline-block text-left">
                                                                            <button
                                                                                onClick={() => setActiveActionDropdown(activeActionDropdown === item.id ? null : item.id)}
                                                                                className="text-[10px] font-bold uppercase text-gray-600 bg-gray-100 px-3 py-1.5 rounded-lg hover:bg-gray-200 transition-all flex items-center gap-1.5"
                                                                            >
                                                                                Actions <span className={`text-[8px] transition-transform ${activeActionDropdown === item.id ? 'rotate-180' : ''}`}>▼</span>
                                                                            </button>
                                                                            {activeActionDropdown === item.id && (
                                                                                <>
                                                                                    <div className="fixed inset-0 z-10" onClick={() => setActiveActionDropdown(null)}></div>
                                                                                    <div className="absolute right-0 mt-1 w-40 bg-white border border-gray-100 rounded-xl shadow-xl z-50 py-1.5 overflow-hidden animate-in fade-in zoom-in duration-100">
                                                                                        <button
                                                                                            onClick={() => {
                                                                                                setSelectedAssignmentForDiscount(item.id);
                                                                                                setShowDiscountModal(true);
                                                                                                setActiveActionDropdown(null);
                                                                                            }}
                                                                                            className="w-full text-left px-4 py-2 text-[10px] font-bold uppercase text-blue-600 hover:bg-blue-50 transition-colors"
                                                                                        >
                                                                                            Apply Discount
                                                                                        </button>
                                                                                        <button
                                                                                            onClick={() => {
                                                                                                if (outstandingLateFee <= 0) return;
                                                                                                setSelectedAssignmentForWaiver(item.id);
                                                                                                setSelectedAssignmentPendingLateFee(outstandingLateFee);
                                                                                                setWaiverForm({ amount: String(outstandingLateFee.toFixed(2)), remarks: "" });
                                                                                                setShowWaiverModal(true);
                                                                                                setActiveActionDropdown(null);
                                                                                            }}
                                                                                            disabled={outstandingLateFee <= 0}
                                                                                            className="w-full text-left px-4 py-2 text-[10px] font-bold uppercase text-orange-600 hover:bg-orange-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                                                                                        >
                                                                                            Waive Late Fee
                                                                                        </button>
                                                                                    </div>
                                                                                </>
                                                                            )}
                                                                        </div>
                                                                    </td>
                                                                )}
                                                            </tr>
                                                            {expandedAssignmentId === item.id && (
                                                                <tr>
                                                                    <td colSpan={canUserManageFees ? 11 : 10} className="bg-gray-50 px-6 py-4 border-b border-gray-100">
                                                                        {loadingAdjustments[item.id] ? (
                                                                            <div className="text-sm text-gray-400 italic">Loading adjustments...</div>
                                                                        ) : adjustmentHistory[item.id]?.length ? (
                                                                            <>
                                                                                <div className="text-[10px] font-black uppercase tracking-widest text-gray-400 mb-2">Adjustments ({adjustmentHistory[item.id].length})</div>
                                                                                <div className="space-y-3 text-sm">
                                                                                    {adjustmentHistory[item.id].map(adj => (
                                                                                        <div key={adj.id} className="border-l-4 border-blue-200 pl-4 bg-white p-3 rounded shadow-sm">
                                                                                            <div className="flex justify-between items-center mb-1">
                                                                                                <span className="font-bold text-gray-700">
                                                                                                    {adj.discountName || adj.type.replaceAll('_', ' ')}
                                                                                                </span>
                                                                                                <span className="font-bold text-blue-600">₹ {adj.amount.toLocaleString()}</span>
                                                                                            </div>
                                                                                            <div className="text-[10px] uppercase font-bold text-gray-400 flex flex-wrap gap-4 mt-2">
                                                                                                {adj.discountType && <span>Type: {adj.discountType}</span>}
                                                                                                {adj.createdByName && <span>By: {adj.createdByName}</span>}
                                                                                                <span>
                                                                                                    {new Date(adj.createdAt).toLocaleString(undefined, {
                                                                                                        year: 'numeric', month: 'short', day: 'numeric',
                                                                                                        hour: '2-digit', minute: '2-digit'
                                                                                                    })}
                                                                                                </span>
                                                                                            </div>
                                                                                            {adj.remarks && (
                                                                                                <div className="text-xs text-gray-500 italic mt-2 border-t pt-2">&quot;{adj.remarks}&quot;</div>
                                                                                            )}
                                                                                        </div>
                                                                                    ))}
                                                                                </div>
                                                                            </>
                                                                        ) : (
                                                                            <div className="text-sm text-gray-400 italic text-center py-2">
                                                                                No adjustments recorded for this assignment.
                                                                            </div>
                                                                        )}
                                                                    </td>
                                                                </tr>
                                                            )}
                                                        </React.Fragment>
                                                    );
                                                })}
                                                {breakdown.length === 0 && (
                                                    <tr>
                                                        <td colSpan={canUserManageFees ? 11 : 10} className="py-8 text-center text-gray-400 italic">No detailed assignments found</td>
                                                    </tr>
                                                )}
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>

                            {/* Transaction Audit Log — directly below fee breakdown, no gap */}
                            <div className="bg-white border rounded-b-2xl shadow-sm overflow-hidden">
                                <div className="p-6 border-b font-black text-xs uppercase tracking-widest text-gray-400 bg-gray-50/50">Transaction Audit Log</div>
                                <div className="overflow-auto">
                                    <table className="w-full text-sm">
                                        <thead className="bg-gray-50 text-gray-400 font-black text-[10px] uppercase tracking-wider border-b">
                                            <tr>
                                                <th className="p-4 text-left">Processing Date</th>
                                                <th className="p-4 text-center">Channel</th>
                                                <th className="p-4 text-left">Narration</th>
                                                <th className="p-4 text-right">Amount</th>
                                                <th className="p-4 text-center w-24">Voucher</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-gray-100">
                                            {history.map(p => (
                                                <tr key={p.id} className="hover:bg-gray-50/50 transition-colors">
                                                    <td className="p-4 font-bold text-gray-700">{p.paymentDate}</td>
                                                    <td className="p-4 text-center">
                                                        <span className="px-2 py-0.5 bg-gray-100 rounded-lg text-[10px] font-black uppercase text-gray-500 border">
                                                            {p.mode}
                                                        </span>
                                                    </td>
                                                    <td className="p-4 text-gray-400 text-xs italic">{p.remarks || "Regular collection"}</td>
                                                    <td className="p-4 text-right font-black text-gray-900">₹ {(p.amountPaid ?? 0).toLocaleString()}</td>
                                                    <td className="p-4 text-center">
                                                        <button
                                                            onClick={() => downloadReceipt(p.id)}
                                                            disabled={isDownloading}
                                                            className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-all disabled:opacity-30"
                                                            title="Download Voucher"
                                                        >
                                                            <svg className="w-5 h-5 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a2 2 0 002 2h12a2 2 0 002-2v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                                                            </svg>
                                                        </button>
                                                    </td>
                                                </tr>
                                            ))}
                                            {history.length === 0 && (
                                                <tr>
                                                    <td colSpan={5} className="p-20 text-center text-gray-300 italic font-medium">
                                                        No historical transactions found for this account.
                                                    </td>
                                                </tr>
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                        </div>
                    </div>
                </div>
            ) : (
                <div className="p-32 text-center bg-gray-50 rounded-3xl border border-dashed border-gray-300 text-gray-400 italic">
                    <div className="max-w-xs mx-auto space-y-4">
                        <p>Initialize billing by selecting a classroom and student from the registry above.</p>
                        <div className="flex justify-center gap-2 opacity-30">
                            <span className="w-2 h-2 rounded-full bg-gray-400"></span>
                            <span className="w-2 h-2 rounded-full bg-gray-400"></span>
                            <span className="w-2 h-2 rounded-full bg-gray-400"></span>
                        </div>
                    </div>
                </div>
            )}

            {/* Discount Modal */}
            <Modal
                isOpen={showDiscountModal}
                onClose={() => {
                    setShowDiscountModal(false);
                    setDiscountForm({ discountId: "", remarks: "" });
                }}
                title="Apply Fee Discount"
                maxWidth="max-w-md"
                footer={
                    <div className="flex gap-2">
                        <button
                            onClick={() => {
                                setShowDiscountModal(false);
                                setDiscountForm({ discountId: "", remarks: "" });
                            }}
                            className="px-6 py-2 rounded-xl border font-medium text-gray-600 hover:bg-gray-50 transition-all"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={applyDiscount}
                            disabled={isApplyingDiscount || !discountForm.discountId}
                            className="px-8 py-2 rounded-xl bg-blue-600 text-white font-bold shadow-lg hover:bg-blue-700 disabled:bg-gray-400 transition-all"
                        >
                            {isApplyingDiscount ? "Applying..." : "Apply"}
                        </button>
                    </div>
                }
            >
                <div className="space-y-4">
                    <p className="text-sm text-gray-500 mb-4">
                        Select an active discount policy to apply to this fee assignment.
                        The discount amount will be deducted from the outstanding principal.
                    </p>
                    <div>
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Discount Policy *</label>
                        <select
                            className="input-ref w-full font-bold"
                            value={discountForm.discountId}
                            onChange={e => setDiscountForm({ ...discountForm, discountId: e.target.value })}
                        >
                            <option value="">Select a discount</option>
                            {discounts.map(d => (
                                <option key={d.id} value={d.id}>
                                    {d.name} ({d.type === 'FLAT' ? `₹${d.amountValue}` : `${d.amountValue}%`})
                                </option>
                            ))}
                        </select>
                        {discounts.length === 0 && (
                            <p className="text-xs text-orange-500 mt-2 font-semibold">
                                No active discount policies found. Create them in Manage Discounts.
                            </p>
                        )}
                    </div>
                    <div>
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Narration / Reason</label>
                        <textarea
                            className="input-ref w-full h-24 text-sm"
                            placeholder="Optional reason for applying this discount..."
                            value={discountForm.remarks}
                            onChange={e => setDiscountForm({ ...discountForm, remarks: e.target.value })}
                        />
                    </div>
                </div>
            </Modal>

            {/* Waive Late Fee Modal */}
            <Modal
                isOpen={showWaiverModal}
                onClose={() => {
                    setShowWaiverModal(false);
                    setSelectedAssignmentForWaiver(null);
                    setSelectedAssignmentPendingLateFee(0);
                    setWaiverForm({ amount: "", remarks: "" });
                }}
                title="Waive Late Fee"
                maxWidth="max-w-md"
                footer={
                    <div className="flex gap-2">
                        <button
                            onClick={() => {
                                setShowWaiverModal(false);
                                setSelectedAssignmentForWaiver(null);
                                setSelectedAssignmentPendingLateFee(0);
                                setWaiverForm({ amount: "", remarks: "" });
                            }}
                            className="px-6 py-2 rounded-xl border font-medium text-gray-600 hover:bg-gray-50 transition-all"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={applyLateFeeWaiver}
                            disabled={
                                isApplyingWaiver
                                || !waiverForm.amount
                                || Number(waiverForm.amount) <= 0
                                || Number(waiverForm.amount) > selectedAssignmentPendingLateFee
                                || !waiverForm.remarks.trim()
                            }
                            className="px-8 py-2 rounded-xl bg-orange-600 text-white font-bold shadow-lg hover:bg-orange-700 disabled:bg-gray-400 transition-all"
                        >
                            {isApplyingWaiver ? "Applying..." : "Apply Waiver"}
                        </button>
                    </div>
                }
            >
                <div className="space-y-4">
                    <p className="text-sm text-gray-500 mb-4">
                        Enter the late fee amount to waive from this assignment.
                    </p>
                    <div>
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Waiver Amount *</label>
                        <input
                            type="number"
                            min="0.01"
                            step="0.01"
                            max={selectedAssignmentPendingLateFee.toFixed(2)}
                            className="input-ref w-full font-bold"
                            value={waiverForm.amount}
                            onChange={e => setWaiverForm({ ...waiverForm, amount: e.target.value })}
                            placeholder="Enter waiver amount"
                        />
                        <p className="text-[11px] text-gray-400 mt-1 ml-1">
                            Maximum waivable: ₹ {selectedAssignmentPendingLateFee.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </p>
                    </div>
                    <div>
                        <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Narration / Reason *</label>
                        <textarea
                            className="input-ref w-full h-24 text-sm"
                            placeholder="Reason for late fee waiver..."
                            value={waiverForm.remarks}
                            onChange={e => setWaiverForm({ ...waiverForm, remarks: e.target.value })}
                        />
                    </div>
                </div>
            </Modal>
        </div>
    );
}
