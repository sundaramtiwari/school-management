"use client";

import { useEffect, useState } from "react";
import { transportApi } from "@/lib/transportApi";
import { api } from "@/lib/api";
import { studentApi } from "@/lib/studentApi";
import { useToast } from "@/components/ui/Toast";
import Modal from "@/components/ui/Modal";
import { Skeleton, TableSkeleton } from "@/components/ui/Skeleton";

type SchoolClass = { id: number; name: string; section: string; session: string };
type Student = { id: number; firstName: string; lastName: string; admissionNumber: string };
type Route = { id: number; name: string };
type PickupPoint = { id: number; name: string; amount: number; frequency: string };

export default function TransportEnrollmentPage() {
    const { showToast } = useToast();

    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [selectedClass, setSelectedClass] = useState<number | "">("");
    const [students, setStudents] = useState<Student[]>([]);
    const [loading, setLoading] = useState(false);

    const [routes, setRoutes] = useState<Route[]>([]);
    const [pickups, setPickups] = useState<PickupPoint[]>([]);
    const [selectedRouteId, setSelectedRouteId] = useState<number | "">("");

    const [showEnrollModal, setShowEnrollModal] = useState(false);
    const [currentStudent, setCurrentStudent] = useState<Student | null>(null);
    const [selectedPickupId, setSelectedPickupId] = useState<number | "">("");
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        loadClasses();
        loadRoutes();
    }, []);

    async function loadClasses() {
        try {
            const res = await api.get("/api/classes?size=100");
            setClasses(res.data.content || []);
        } catch {
            showToast("Failed to load classes", "error");
        }
    }

    async function loadRoutes() {
        try {
            const res = await transportApi.getAllRoutes();
            setRoutes(res.data || []);
        } catch {
            showToast("Failed to load transport lines", "error");
        }
    }

    async function onClassChange(e: any) {
        const clsId = e.target.value;
        setSelectedClass(clsId);
        if (clsId) {
            try {
                setLoading(true);
                const res = await studentApi.byClass(Number(clsId), 0, 100);
                setStudents(res.data.content || []);
            } catch {
                showToast("Failed to pull student list", "error");
            } finally {
                setLoading(false);
            }
        } else {
            setStudents([]);
        }
    }

    async function openEnrollModal(student: Student) {
        setCurrentStudent(student);
        setSelectedRouteId("");
        setPickups([]);
        setSelectedPickupId("");
        setShowEnrollModal(true);
    }

    async function onRouteSelectChange(e: any) {
        const routeId = e.target.value;
        setSelectedRouteId(routeId);
        setSelectedPickupId("");
        if (routeId) {
            try {
                const res = await transportApi.getPickupsByRoute(Number(routeId));
                setPickups(res.data || []);
            } catch {
                showToast("Failed to load pickup points", "error");
            }
        } else {
            setPickups([]);
        }
    }

    async function enrollStudent() {
        if (!currentStudent || !selectedPickupId || !selectedClass) return;
        const cls = classes.find(c => c.id == selectedClass);
        if (!cls) return;

        try {
            setIsSaving(true);
            await transportApi.enroll({
                studentId: currentStudent.id,
                pickupPointId: Number(selectedPickupId),
                session: cls.session,
            });
            showToast("Student enrolled in transport!", "success");
            setShowEnrollModal(false);
        } catch (e: any) {
            showToast("Enrollment failed: " + (e.response?.data?.message || e.message), "error");
        } finally {
            setIsSaving(false);
        }
    }

    return (
        <div className="space-y-6">
            <header>
                <h1 className="text-3xl font-bold text-gray-800">Transport Enrollment</h1>
                <p className="text-gray-500">Assign students to transport routes for the current session.</p>
            </header>

            <div className="bg-white p-6 border rounded-2xl shadow-sm flex flex-wrap gap-4 items-end">
                <div className="flex-1 min-w-[300px]">
                    <label className="block text-xs font-bold uppercase text-gray-400 mb-2 ml-1">Select Academic Class</label>
                    <select className="input-ref" value={selectedClass} onChange={onClassChange}>
                        <option value="">Choose Class</option>
                        {classes.map(c => <option key={c.id} value={c.id}>{c.name} {c.section} ({c.session})</option>)}
                    </select>
                </div>
            </div>

            {loading ? (
                <div className="bg-white p-8 rounded-2xl border">
                    <TableSkeleton rows={10} cols={4} />
                </div>
            ) : selectedClass ? (
                <div className="bg-white border rounded-2xl shadow-sm overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-gray-50 text-gray-600 font-bold border-b">
                            <tr>
                                <th className="p-4 text-left">Student</th>
                                <th className="p-4 text-center">Admission No</th>
                                <th className="p-4 text-center w-48">Transport Status</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100 font-medium">
                            {students.map(s => (
                                <tr key={s.id} className="hover:bg-gray-50/50 transition-colors">
                                    <td className="p-4 text-gray-800 font-bold">{s.firstName} {s.lastName}</td>
                                    <td className="p-4 text-center font-mono text-gray-400 text-xs">{s.admissionNumber}</td>
                                    <td className="p-4 text-center">
                                        <button
                                            onClick={() => openEnrollModal(s)}
                                            className="px-4 py-1.5 bg-blue-50 text-blue-600 rounded-lg text-xs font-bold border border-blue-100 hover:bg-blue-600 hover:text-white transition-all shadow-sm shadow-blue-50"
                                        >
                                            Enroll in Bus
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {students.length === 0 && (
                                <tr>
                                    <td colSpan={3} className="p-20 text-center text-gray-400 italic">
                                        No student records found for this class identifier.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            ) : (
                <div className="p-20 text-center bg-gray-50 rounded-2xl border border-dashed text-gray-400 italic">
                    Select a class above to manage transport enrollments.
                </div>
            )}

            {/* Enrollment Modal */}
            <Modal
                isOpen={showEnrollModal}
                onClose={() => setShowEnrollModal(false)}
                title={`Enroll ${currentStudent?.firstName} in Transport`}
                footer={
                    <div className="flex gap-2">
                        <button onClick={() => setShowEnrollModal(false)} className="px-6 py-2 border rounded-xl font-medium text-gray-600 hover:bg-gray-50">Cancel</button>
                        <button
                            onClick={enrollStudent}
                            disabled={isSaving || !selectedPickupId}
                            className="px-8 py-2 bg-blue-600 text-white font-bold rounded-xl shadow-lg hover:bg-blue-700 disabled:bg-gray-400 transition-all"
                        >
                            {isSaving ? "Enrolling..." : "Confirm Enrollment"}
                        </button>
                    </div>
                }
            >
                <div className="space-y-6">
                    <div className="p-4 bg-orange-50 border border-orange-100 rounded-xl text-xs text-orange-800 font-medium flex gap-3">
                        <span className="text-xl">💡</span>
                        <p>Enrollment will automatically assign the recurring transport fee to the student's ledger for the session.</p>
                    </div>

                    <div className="space-y-4">
                        <div>
                            <label className="block text-[10px] font-black uppercase text-gray-400 mb-1.5 ml-1">Route Line</label>
                            <select
                                className="input-ref font-bold"
                                value={selectedRouteId}
                                onChange={onRouteSelectChange}
                            >
                                <option value="">Select Transport Route</option>
                                {routes.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
                            </select>
                        </div>

                        {selectedRouteId && (
                            <div>
                                <label className="block text-[10px] font-black uppercase text-gray-400 mb-1.5 ml-1">Pickup Stop</label>
                                <select
                                    className="input-ref font-bold border-blue-200 bg-blue-50/20"
                                    value={selectedPickupId}
                                    onChange={e => setSelectedPickupId(e.target.value)}
                                >
                                    <option value="">Select Stop & Fee</option>
                                    {pickups.map(p => (
                                        <option key={p.id} value={p.id}>
                                            {p.name} - ₹{p.amount} ({p.frequency})
                                        </option>
                                    ))}
                                </select>
                                {pickups.length === 0 && (
                                    <p className="text-[10px] text-red-500 mt-1 italic font-bold">No stops available for this route.</p>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </Modal>
        </div>
    );
}
