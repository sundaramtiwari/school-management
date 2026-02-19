"use client";

import { ChangeEvent, useCallback, useEffect, useState } from "react";
import { transportApi } from "@/lib/transportApi";
import { api } from "@/lib/api";
import { studentApi } from "@/lib/studentApi";
import { useToast } from "@/components/ui/Toast";
import { useSession } from "@/context/SessionContext";
import { useAuth } from "@/context/AuthContext";
import Modal from "@/components/ui/Modal";
import { TableSkeleton } from "@/components/ui/Skeleton";

type SchoolClass = { id: number; name: string; section: string; sessionId: number };
type Student = { id: number; firstName: string; lastName: string; admissionNumber: string };
type Route = { id: number; name: string; capacity: number; currentStrength: number };
type PickupPoint = { id: number; name: string; amount: number; frequency: string };
type Enrollment = { id: number; studentId: number; pickupPointId: number; pickupPointName?: string; routeId?: number; routeName?: string; active: boolean };

export default function TransportEnrollmentPage() {
    const { user } = useAuth();
    const { showToast } = useToast();
    const { currentSession } = useSession();
    const canMutateTransportEnrollment = user?.role === "SCHOOL_ADMIN" || user?.role === "SUPER_ADMIN" || user?.role === "ACCOUNTANT";

    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [selectedClass, setSelectedClass] = useState<number | "">("");
    const [students, setStudents] = useState<Student[]>([]);
    const [enrollments, setEnrollments] = useState<Record<number, Enrollment>>({});
    const [loading, setLoading] = useState(false);
    const [loadingStudents, setLoadingStudents] = useState<Record<number, boolean>>({});

    const [routes, setRoutes] = useState<Route[]>([]);
    const [pickups, setPickups] = useState<PickupPoint[]>([]);
    const [selectedRouteId, setSelectedRouteId] = useState<number | "">("");

    const [showEnrollModal, setShowEnrollModal] = useState(false);
    const [showUnenrollModal, setShowUnenrollModal] = useState(false);
    const [currentStudent, setCurrentStudent] = useState<Student | null>(null);
    const [selectedPickupId, setSelectedPickupId] = useState<number | "">("");
    const [isSaving, setIsSaving] = useState(false);

    const loadClasses = useCallback(async () => {
        try {
            const res = await api.get("/api/classes/mine?size=100");
            setClasses(res.data.content || []);
        } catch {
            showToast("Failed to load classes", "error");
        }
    }, [showToast]);

    const loadRoutes = useCallback(async () => {
        try {
            const res = await transportApi.getAllRoutes();
            setRoutes(res.data || []);
        } catch {
            showToast("Failed to load transport lines", "error");
        }
    }, [showToast]);

    useEffect(() => {
        loadClasses();
        loadRoutes();
    }, [currentSession, loadClasses, loadRoutes]);

    async function onClassChange(e: ChangeEvent<HTMLSelectElement>) {
        const clsId = e.target.value;
        setSelectedClass(clsId);
        if (clsId && currentSession) {
            try {
                setLoading(true);
                const studentRes = await studentApi.byClass(Number(clsId), 0, 100);
                const studentList = studentRes.data.content || [];
                setStudents(studentList);

                // Fetch batch transport status
                if (studentList.length > 0) {
                    const ids = studentList.map((s: Student) => s.id);
                    const enrollRes = await transportApi.getBatchStatus(ids, currentSession.id);
                    const enrollMap: Record<number, Enrollment> = {};
                    enrollRes.data.forEach((e: Enrollment) => {
                        enrollMap[e.studentId] = e;
                    });
                    setEnrollments(enrollMap);
                } else {
                    setEnrollments({});
                }
            } catch {
                showToast("Failed to pull student list or status", "error");
            } finally {
                setLoading(false);
            }
        } else {
            setStudents([]);
            setEnrollments({});
        }
    }

    async function openEnrollModal(student: Student) {
        setCurrentStudent(student);
        setSelectedRouteId("");
        setPickups([]);
        setSelectedPickupId("");
        setShowEnrollModal(true);
    }

    async function openUnenrollModal(student: Student) {
        setCurrentStudent(student);
        setShowUnenrollModal(true);
    }

    async function onRouteSelectChange(e: ChangeEvent<HTMLSelectElement>) {
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
        if (!canMutateTransportEnrollment) return;
        if (!currentStudent || !selectedPickupId || !selectedClass || !currentSession) return;

        try {
            setIsSaving(true);
            setLoadingStudents(prev => ({ ...prev, [currentStudent.id]: true }));
            const res = await transportApi.enroll({
                studentId: currentStudent.id,
                pickupPointId: Number(selectedPickupId),
                sessionId: currentSession.id,
            });

            // Update enrollment state
            setEnrollments(prev => ({ ...prev, [currentStudent.id]: res.data }));

            // Refresh routes to reflect updated capacity
            loadRoutes();

            showToast("Student enrolled in transport!", "success");
            setShowEnrollModal(false);
        } catch (e: unknown) {
            const message = e instanceof Error ? e.message : "Enrollment request failed";
            showToast("Enrollment failed: " + message, "error");
        } finally {
            setIsSaving(false);
            setLoadingStudents(prev => ({ ...prev, [currentStudent.id]: false }));
        }
    }

    async function unenrollStudent() {
        if (!canMutateTransportEnrollment) return;
        if (!currentStudent || !currentSession) return;

        try {
            setIsSaving(true);
            setLoadingStudents(prev => ({ ...prev, [currentStudent.id]: true }));
            await transportApi.unenroll(currentStudent.id, currentSession.id);

            // Remove from enrollment state
            setEnrollments(prev => {
                const updated = { ...prev };
                delete updated[currentStudent.id];
                return updated;
            });

            // Refresh routes to reflect restored capacity
            loadRoutes();

            showToast("Student unenrolled successfully", "success");
            setShowUnenrollModal(false);
        } catch (e: unknown) {
            const message = e instanceof Error ? e.message : "Unenrollment request failed";
            showToast("Unenrollment failed: " + message, "error");
        } finally {
            setIsSaving(false);
            setLoadingStudents(prev => ({ ...prev, [currentStudent.id]: false }));
        }
    }

    return (
        <div className="space-y-6">
            <header>
                <h1 className="text-3xl font-bold text-gray-800">Transport Enrollment</h1>
                <p className="text-gray-500">Assign students to transport routes for <span className="text-blue-600 font-bold">{currentSession?.name || "current session"}</span>.</p>
            </header>

            <div className="bg-white p-6 border rounded-2xl shadow-sm flex flex-wrap gap-4 items-end">
                <div className="flex-1 min-w-[300px]">
                    <label className="block text-xs font-bold uppercase text-gray-400 mb-2 ml-1">Select Academic Class</label>
                    <select className="input-ref" value={selectedClass} onChange={onClassChange}>
                        <option value="">Choose Class</option>
                        {classes.map(c => <option key={c.id} value={c.id}>{c.name} {c.section}</option>)}
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
                                <th className="p-4 text-center">Current Route</th>
                                <th className="p-4 text-center w-48">Action</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100 font-medium">
                            {students.map(s => {
                                const enrollment = enrollments[s.id];
                                const isLoading = loadingStudents[s.id];

                                return (
                                    <tr key={s.id} className="hover:bg-gray-50/50 transition-colors">
                                        <td className="p-4 text-gray-800 font-bold">{s.firstName} {s.lastName}</td>
                                        <td className="p-4 text-center font-mono text-gray-400 text-xs">{s.admissionNumber}</td>
                                        <td className="p-4 text-center">
                                            {enrollment ? (
                                                <div className="flex flex-col items-center">
                                                    <span className="text-blue-600 font-bold text-xs">{enrollment.routeName}</span>
                                                    <span className="text-[10px] text-gray-400">{enrollment.pickupPointName}</span>
                                                </div>
                                            ) : (
                                                <span className="text-gray-300 italic text-xs">Not Enrolled</span>
                                            )}
                                        </td>
                                        <td className="p-4 text-center">
                                            {enrollment ? (
                                                <button
                                                    onClick={() => openUnenrollModal(s)}
                                                    disabled={isLoading || !canMutateTransportEnrollment}
                                                    className="px-4 py-1.5 bg-red-50 text-red-600 rounded-lg text-xs font-bold border border-red-100 hover:bg-red-600 hover:text-white transition-all shadow-sm disabled:opacity-50"
                                                >
                                                    {isLoading ? "Wait..." : "Unenroll"}
                                                </button>
                                            ) : (
                                                <button
                                                    onClick={() => openEnrollModal(s)}
                                                    disabled={isLoading || !canMutateTransportEnrollment}
                                                    className="px-4 py-1.5 bg-blue-50 text-blue-600 rounded-lg text-xs font-bold border border-blue-100 hover:bg-blue-600 hover:text-white transition-all shadow-sm shadow-blue-50 disabled:opacity-50"
                                                >
                                                    {isLoading ? "Wait..." : "Enroll in Bus"}
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                );
                            })}
                            {students.length === 0 && (
                                <tr>
                                    <td colSpan={4} className="p-20 text-center text-gray-400 italic">
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
                            className="px-8 py-2 bg-blue-600 text-white font-bold rounded-xl shadow-lg hover:bg-blue-700 disabled:bg-gray-400 transition-all font-outfit"
                        >
                            {isSaving ? "Enrolling..." : "Confirm Enrollment"}
                        </button>
                    </div>
                }
            >
                <div className="space-y-6">
                    <div className="p-4 bg-orange-50 border border-orange-100 rounded-xl text-xs text-orange-800 font-medium flex gap-3">
                        <span className="text-xl">💡</span>
                        <p>Enrollment will automatically assign the recurring transport fee to the student&apos;s ledger for the session.</p>
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
                                {routes.map(r => (
                                    <option
                                        key={r.id}
                                        value={r.id}
                                        disabled={r.currentStrength >= r.capacity}
                                    >
                                        {r.name} ({r.currentStrength}/{r.capacity} seats filled) {r.currentStrength >= r.capacity ? "- FULL" : ""}
                                    </option>
                                ))}
                            </select>
                        </div>

                        {selectedRouteId && (
                            <div>
                                <label className="block text-[10px] font-black uppercase text-gray-400 mb-1.5 ml-1">Pickup Stop</label>
                                <select
                                    className="input-ref font-bold border-blue-200 bg-blue-50/20"
                                    value={selectedPickupId}
                                    onChange={e => setSelectedPickupId(e.target.value ? Number(e.target.value) : "")}
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

            {/* Unenrollment Confirmation Modal */}
            <Modal
                isOpen={showUnenrollModal}
                onClose={() => setShowUnenrollModal(false)}
                title="Confirm Unenrollment"
                footer={
                    <div className="flex gap-2">
                        <button onClick={() => setShowUnenrollModal(false)} className="px-6 py-2 border rounded-xl font-medium text-gray-600 hover:bg-gray-50">Cancel</button>
                        <button
                            onClick={unenrollStudent}
                            disabled={isSaving}
                            className="px-8 py-2 bg-red-600 text-white font-bold rounded-xl shadow-lg hover:bg-red-700 disabled:bg-gray-400 transition-all"
                        >
                            {isSaving ? "Unenrolling..." : "Confirm Unenrollment"}
                        </button>
                    </div>
                }
            >
                <div className="space-y-4">
                    <div className="p-4 bg-red-50 border border-red-100 rounded-xl text-red-800 flex gap-3">
                        <span className="text-xl">⚠️</span>
                        <div className="text-xs font-medium space-y-2">
                            <p className="font-bold">Are you sure you want to unenroll {currentStudent?.firstName} from transport?</p>
                            <ul className="list-disc list-inside space-y-1">
                                <li>One seat will be freed in the route: <b>{enrollments[currentStudent?.id || 0]?.routeName}</b></li>
                                <li>Future transport fee assignments will be deactivated.</li>
                                <li>Historical payment records will be preserved.</li>
                            </ul>
                        </div>
                    </div>
                    <p className="text-sm text-gray-500">
                        This action is session-locked and follows strict multi-tenant safety rules.
                    </p>
                </div>
            </Modal>
        </div>
    );
}
