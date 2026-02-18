"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";
import { Skeleton, TableSkeleton } from "@/components/ui/Skeleton";
import { useSession } from "@/context/SessionContext";
import { useAuth } from "@/context/AuthContext";

type SchoolClass = { id: number; name: string; section: string; sessionId: number };
type Student = { id: number; firstName: string; lastName: string; admissionNumber: string };
type FeeSummary = {
    totalFee: number;
    totalPaid: number;
    pendingFee: number;
    totalLateFeeAccrued: number;
    totalLateFeePaid: number;
    status: string
};
type Payment = { id: number; amountPaid: number; paymentDate: string; mode: string; remarks: string };

export default function FeeCollectPage() {
    const { user } = useAuth();
    const { showToast } = useToast();
    const { currentSession } = useSession();

    const canCollectFees = user?.role === "ACCOUNTANT" || user?.role === "SCHOOL_ADMIN" || user?.role === "SUPER_ADMIN" || user?.role === "PLATFORM_ADMIN";

    /* -------- State -------- */
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [selectedClass, setSelectedClass] = useState<number | "">("");

    const [students, setStudents] = useState<Student[]>([]);
    const [selectedStudent, setSelectedStudent] = useState<number | "">("");

    const [summary, setSummary] = useState<FeeSummary | null>(null);
    const [history, setHistory] = useState<Payment[]>([]);

    const [loading, setLoading] = useState(false);
    const [loadingStudents, setLoadingStudents] = useState(false);
    const [isProcessing, setIsProcessing] = useState(false);
    const [isDownloading, setIsDownloading] = useState(false);

    const [paymentAmount, setPaymentAmount] = useState("");
    const [paymentMode, setPaymentMode] = useState("CASH");
    const [remarks, setRemarks] = useState("");
    const [months, setMonths] = useState(1);

    /* -------- Initial Load -------- */
    useEffect(() => {
        loadClasses();
    }, [currentSession]);

    async function loadClasses() {
        try {
            const res = await api.get("/api/classes/mine?size=100");
            setClasses(res.data.content || []);
        } catch {
            showToast("Failed to initialize billing classes", "error");
        }
    }

    /* -------- Handlers -------- */
    async function onClassChange(e: any) {
        const clsId = e.target.value;
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

    async function onStudentChange(e: any) {
        const stdId = e.target.value;
        setSelectedStudent(stdId);
        setSummary(null);
        setHistory([]);
        if (stdId) loadStudentData(stdId);
    }

    async function loadStudentData(stdId: number) {
        if (!currentSession) return;
        try {
            setLoading(true);
            const [sumRes, histRes] = await Promise.all([
                api.get(`/api/fees/summary/students/${stdId}?sessionId=${currentSession.id}`),
                api.get(`/api/fees/payments/students/${stdId}`)
            ]);

            setSummary(sumRes.data);
            setHistory(histRes.data || []);
        } catch {
            showToast("Billing synchronization failed", "error");
        } finally {
            setLoading(false);
        }
    }

    async function makePayment() {
        if (!selectedStudent || !paymentAmount) return;

        try {
            setIsProcessing(true);
            await api.post("/api/fees/payments", {
                studentId: selectedStudent,
                amountPaid: Number(paymentAmount),
                mode: paymentMode,
                remarks,
                paymentDate: new Date().toISOString().split('T')[0]
            });

            showToast("Payment Processed Successfully!", "success");
            setPaymentAmount("");
            setRemarks("");
            loadStudentData(Number(selectedStudent));
        } catch (e: any) {
            showToast("Processing failed: " + (e.response?.data?.message || e.message), "error");
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
            const res = await api.get(`/api/fees/challan/student/${selectedStudent}?sessionId=${currentSession.id}&months=${months}`, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `fee_challan_${selectedStudent}_${months}m.pdf`);
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

    return (
        <div className="space-y-6">
            <header>
                <h1 className="text-3xl font-bold text-gray-800">Billing & Collections</h1>
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
                    <select
                        className="input-ref font-bold"
                        value={selectedStudent}
                        onChange={onStudentChange}
                        disabled={!selectedClass || loadingStudents}
                    >
                        <option value="">{loadingStudents ? "Syncing Students..." : "Select Student to Settle"}</option>
                        {students.map(s => <option key={s.id} value={s.id}>{s.firstName} {s.lastName} ({s.admissionNumber})</option>)}
                    </select>
                </div>
            </div>

            {loading ? (
                <div className="bg-white p-12 rounded-2xl border text-center text-gray-400 italic">
                    Syncing financial data with ledger...
                </div>
            ) : (selectedStudent && summary) ? (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {/* Summary Card */}
                    <div className="lg:col-span-1 space-y-6">
                        <div className="bg-gray-900 text-white rounded-2xl p-8 shadow-2xl relative overflow-hidden">
                            <div className="absolute top-0 right-0 p-4 opacity-10 text-6xl">₹</div>
                            <h3 className="text-xs font-black uppercase tracking-widest text-gray-400 mb-2">Outstanding Balance</h3>
                            <p className="text-5xl font-black">₹ {(summary.pendingFee ?? 0).toLocaleString()}</p>

                            <div className="mt-6 flex flex-col gap-2 text-[10px] font-bold uppercase tracking-tight border-t border-white/10 pt-4">
                                <div className="flex justify-between">
                                    <span className="text-gray-400">Principal Amount:</span>
                                    <span className="text-white">₹ {summary.totalFee.toLocaleString()}</span>
                                </div>
                                <div className="flex justify-between">
                                    <span className="text-gray-400">Accrued Late Fees:</span>
                                    <span className="text-orange-400">₹ {(summary.totalLateFeeAccrued ?? 0).toLocaleString()}</span>
                                </div>
                                <div className="flex justify-between border-t border-white/5 pt-2 mt-1">
                                    <span className="text-gray-400">Total Collected:</span>
                                    <span className="text-green-400">₹ {summary.totalPaid.toLocaleString()}</span>
                                </div>
                            </div>

                            <div className="mt-8 pt-6 border-t border-white/10 space-y-4">
                                <div>
                                    <label className="block text-[10px] font-black uppercase text-gray-400 mb-1.5 ml-1">Billing Duration (Months)</label>
                                    <select
                                        className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2 text-sm font-bold text-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                                        value={months}
                                        onChange={e => setMonths(Number(e.target.value))}
                                    >
                                        {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map(m => (
                                            <option key={m} value={m} className="bg-gray-900 text-white">{m} Month{m > 1 ? 's' : ''}</option>
                                        ))}
                                    </select>
                                </div>
                                <button
                                    onClick={downloadChallan}
                                    disabled={isDownloading}
                                    className="w-full bg-white/10 hover:bg-white/20 text-white py-3 rounded-xl font-bold transition-all flex items-center justify-center gap-2 border border-white/5 disabled:opacity-50"
                                >
                                    {isDownloading ? "⏳ Generating..." : "📄 Generate Academic Challan"}
                                </button>
                            </div>
                        </div>

                        {/* Payment Entry */}
                        <div className="bg-white border rounded-2xl p-6 shadow-sm space-y-5">
                            <h3 className="font-black text-gray-800 border-b pb-4 text-xs uppercase tracking-widest">Post Payment Transaction</h3>
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-[10px] font-black uppercase text-gray-400 mb-1 ml-1">Collection Amount *</label>
                                    <div className="relative">
                                        <span className="absolute left-4 top-1/2 -translate-y-1/2 font-bold text-gray-400">₹</span>
                                        <input
                                            type="number"
                                            placeholder="0.00"
                                            className="input-ref pl-10 text-xl font-black"
                                            value={paymentAmount}
                                            onChange={e => setPaymentAmount(e.target.value)}
                                        />
                                    </div>
                                </div>
                                <div className="grid grid-cols-2 gap-3">
                                    <div className="col-span-2">
                                        <label className="block text-[10px] font-black uppercase text-gray-400 mb-1 ml-1">Payment Channel</label>
                                        <select className="input-ref font-bold" value={paymentMode} onChange={e => setPaymentMode(e.target.value)}>
                                            <option value="CASH">Liquid Cash</option>
                                            <option value="ONLINE">Digital/UPI</option>
                                            <option value="BANK_TRANSFER">Bank Transfer</option>
                                            <option value="CHEQUE">Banker's Cheque</option>
                                        </select>
                                    </div>
                                </div>
                                <div>
                                    <label className="block text-[10px] font-black uppercase text-gray-400 mb-1 ml-1">Internal Remarks</label>
                                    <textarea
                                        placeholder="Reference or narration..."
                                        className="input-ref h-20 text-sm"
                                        value={remarks}
                                        onChange={e => setRemarks(e.target.value)}
                                    />
                                </div>
                                {canCollectFees ? (
                                    <button
                                        onClick={makePayment}
                                        disabled={!paymentAmount || isProcessing}
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

                    {/* History Table */}
                    <div className="lg:col-span-2 bg-white border rounded-2xl shadow-sm overflow-hidden flex flex-col">
                        <div className="p-6 border-b font-black text-xs uppercase tracking-widest text-gray-400 bg-gray-50/50">Transaction Audit log</div>
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
        </div>
    );
}
