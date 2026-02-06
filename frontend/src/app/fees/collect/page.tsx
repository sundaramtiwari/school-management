"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";

type SchoolClass = { id: number; name: string; section: string; session: string };
type Student = { id: number; firstName: string; lastName: string; admissionNumber: string };
type FeeSummary = { totalFee: number; totalPaid: number; balance: number; status: string };
type Payment = { id: number; amount: number; paymentDate: string; paymentMode: string; remarks: string };

export default function FeeCollectPage() {

    /* -------- State -------- */
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [selectedClass, setSelectedClass] = useState<number | "">("");

    const [students, setStudents] = useState<Student[]>([]);
    const [selectedStudent, setSelectedStudent] = useState<number | "">("");

    const [summary, setSummary] = useState<FeeSummary | null>(null);
    const [history, setHistory] = useState<Payment[]>([]);

    const [loading, setLoading] = useState(false);
    const [paymentAmount, setPaymentAmount] = useState("");
    const [paymentMode, setPaymentMode] = useState("CASH");
    const [remarks, setRemarks] = useState("");

    /* -------- Initial Load -------- */
    useEffect(() => {
        loadClasses();
    }, []);

    async function loadClasses() {
        try {
            const res = await api.get("/api/classes?size=100");
            setClasses(res.data.content || []);
        } catch (e) { console.error(e); }
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
                // Using existing student endpoints. Assuming /api/students/by-class/{id} works or similar
                // Actually StudentApi uses: /api/students/by-class/{classId}
                const res = await api.get(`/api/students/by-class/${clsId}?size=100`);
                setStudents(res.data.content || []);
            } catch (e) { console.error(e); }
        }
    }

    async function onStudentChange(e: any) {
        const stdId = e.target.value;
        setSelectedStudent(stdId);
        setSummary(null);
        setHistory([]);

        if (stdId) {
            loadStudentData(stdId);
        }
    }

    async function loadStudentData(stdId: number) {
        try {
            setLoading(true);
            // 1. Get Summary
            // Need to know session? Usually defaults to current or we pass it.
            // Let's assume passed session from Class or fixed. 
            const cls = classes.find(c => c.id == selectedClass);
            const session = cls ? cls.session : "2024-25";

            const sumRes = await api.get(`/api/fees/summary/students/${stdId}?session=${session}`);
            setSummary(sumRes.data);

            // 2. Get History
            const histRes = await api.get(`/api/fees/payments/students/${stdId}`);
            setHistory(histRes.data || []);

        } catch (e) {
            console.error("Failed to load fee data", e);
        } finally {
            setLoading(false);
        }
    }

    async function makePayment() {
        if (!selectedStudent || !paymentAmount) return;

        try {
            await api.post("/api/fees/payments", {
                studentId: selectedStudent,
                amount: Number(paymentAmount),
                paymentMode,
                remarks,
                paymentDate: new Date().toISOString().split('T')[0] // today
            });

            alert("Payment Successful");
            setPaymentAmount("");
            setRemarks("");
            loadStudentData(Number(selectedStudent)); // Refresh
        } catch (e: any) {
            alert("Payment failed: " + (e.response?.data?.message || e.message));
        }
    }

    async function downloadReceipt(pid: number) {
        try {
            const res = await api.get(`/api/fees/payments/${pid}/receipt`, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `receipt_${pid}.pdf`);
            document.body.appendChild(link);
            link.click();
        } catch (e) {
            alert("Failed to download receipt");
        }
    }

    /* -------- UI -------- */
    return (
        <div className="space-y-6">
            <h2 className="text-xl font-bold">Collect Fees</h2>

            {/* Selection Panel */}
            <div className="bg-white p-4 border rounded shadow-sm flex gap-4">
                <div className="w-1/3">
                    <label className="block text-sm font-medium mb-1">Class</label>
                    <select className="input w-full" value={selectedClass} onChange={onClassChange}>
                        <option value="">Select Class...</option>
                        {classes.map(c => <option key={c.id} value={c.id}>{c.name} {c.section}</option>)}
                    </select>
                </div>
                <div className="w-1/3">
                    <label className="block text-sm font-medium mb-1">Student</label>
                    <select className="input w-full" value={selectedStudent} onChange={onStudentChange} disabled={!selectedClass}>
                        <option value="">Select Student...</option>
                        {students.map(s => <option key={s.id} value={s.id}>{s.firstName} {s.lastName} ({s.admissionNumber})</option>)}
                    </select>
                </div>
            </div>

            {loading && <p>Loading data...</p>}

            {selectedStudent && summary && !loading && (
                <div className="grid grid-cols-3 gap-6">

                    {/* Summary Card */}
                    <div className="col-span-1 space-y-4">
                        <div className="bg-blue-50 border border-blue-100 p-4 rounded text-center">
                            <h3 className="text-blue-800 font-semibold mb-2">Total Dues</h3>
                            <p className="text-3xl font-bold text-blue-900">₹ {summary.balance}</p>
                            <div className="text-xs text-blue-600 mt-1 flex justify-between px-4">
                                <span>Total: {summary.totalFee}</span>
                                <span>Paid: {summary.totalPaid}</span>
                            </div>
                        </div>

                        {/* Payment Form */}
                        <div className="bg-white border p-4 rounded">
                            <h3 className="font-semibold mb-3 border-b pb-2">Record Payment</h3>
                            <div className="space-y-3">
                                <input
                                    type="number"
                                    placeholder="Amount *"
                                    className="input w-full"
                                    value={paymentAmount}
                                    onChange={e => setPaymentAmount(e.target.value)}
                                />
                                <select
                                    className="input w-full"
                                    value={paymentMode}
                                    onChange={e => setPaymentMode(e.target.value)}
                                >
                                    <option value="CASH">Cash</option>
                                    <option value="ONLINE">Online/UPI</option>
                                    <option value="CHEQUE">Cheque</option>
                                </select>
                                <textarea
                                    placeholder="Remarks"
                                    className="input w-full"
                                    value={remarks}
                                    onChange={e => setRemarks(e.target.value)}
                                />
                                <button
                                    onClick={makePayment}
                                    disabled={!paymentAmount}
                                    className="w-full bg-green-600 text-white py-2 rounded font-medium disabled:opacity-50"
                                >
                                    Collect Payment
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* History */}
                    <div className="col-span-2 bg-white border rounded">
                        <div className="p-3 border-b font-semibold bg-gray-50">Payment History</div>
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="border-b">
                                    <th className="p-3 text-left">Date</th>
                                    <th className="p-3 text-left">Mode</th>
                                    <th className="p-3 text-left">Remarks</th>
                                    <th className="p-3 text-right">Amount</th>
                                    <th className="p-3 text-center">Receipt</th>
                                </tr>
                            </thead>
                            <tbody>
                                {history.map(p => (
                                    <tr key={p.id} className="border-b last:border-0 hover:bg-gray-50">
                                        <td className="p-3">{p.paymentDate}</td>
                                        <td className="p-3"><span className="bg-gray-100 px-2 py-1 rounded text-xs">{p.paymentMode}</span></td>
                                        <td className="p-3 text-gray-500">{p.remarks || "-"}</td>
                                        <td className="p-3 text-right font-medium">₹ {p.amount}</td>
                                        <td className="p-3 text-center">
                                            <button
                                                onClick={() => downloadReceipt(p.id)}
                                                className="text-blue-600 hover:text-blue-800 text-xs"
                                            >
                                                Download
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                {history.length === 0 && (
                                    <tr><td colSpan={5} className="p-6 text-center text-gray-400">No payments recorded</td></tr>
                                )}
                            </tbody>
                        </table>
                    </div>

                </div>
            )}

        </div>
    );
}
