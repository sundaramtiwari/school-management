"use client";

import { ChangeEvent, useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "@/context/SessionContext";
import { api } from "@/lib/api";
import { examApi } from "@/lib/examApi";
import { useToast } from "@/components/ui/Toast";
import { TableSkeleton } from "@/components/ui/Skeleton";
import ExamCreationModal from "@/components/exams/ExamCreationModal";
import ExamDetailView from "@/components/exams/ExamDetailView";
import { useAuth } from "@/context/AuthContext";

type SchoolClass = {
    id: number;
    name: string;
    section: string;
};

type Exam = {
    id: number;
    name: string;
    schoolId: number;
    sessionId: number;
    examType: string;
    startDate?: string;
    endDate?: string;
    active: boolean;
    status: 'DRAFT' | 'PUBLISHED' | 'LOCKED';
};

export default function ExamsPage() {
    const router = useRouter();
    const { user } = useAuth();
    const { showToast } = useToast();
    const { currentSession, isSessionLoading, hasClasses } = useSession();

    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [selectedClass, setSelectedClass] = useState<number | "">("");
    const [loadingClasses, setLoadingClasses] = useState(true);

    const canCreateExam = user?.role === "SCHOOL_ADMIN" || user?.role === "SUPER_ADMIN" || user?.role === "PLATFORM_ADMIN" || user?.role === "TEACHER";

    const [exams, setExams] = useState<Exam[]>([]);
    const [loadingExams, setLoadingExams] = useState(false);

    const [showAddModal, setShowAddModal] = useState(false);
    const [selectedExam, setSelectedExam] = useState<Exam | null>(null);

    /* ---------------- Gating ---------------- */

    useEffect(() => {
        if (!isSessionLoading) {
            if (!currentSession) {
                // Handled in UI by showing message
            } else if (!hasClasses) {
                showToast("No classes found. Redirecting to Classes module...", "warning");
                router.push("/classes");
            }
        }
    }, [currentSession, isSessionLoading, hasClasses, router, showToast]);

    /* ---------------- Load Data ---------------- */

    const loadClasses = useCallback(async () => {
        try {
            setLoadingClasses(true);
            const res = await api.get("/api/classes/mine");
            // For some reason /api/classes/mine returns Page<SchoolClassDto>
            setClasses(res.data.content || []);
        } catch {
            showToast("Failed to load classes", "error");
        } finally {
            setLoadingClasses(false);
        }
    }, [showToast]);

    useEffect(() => {
        if (currentSession) {
            void loadClasses();
        }
    }, [currentSession, loadClasses]);

    async function loadExams(classId: number) {
        if (!currentSession) return;
        try {
            setLoadingExams(true);
            const res = await examApi.listByClass(classId, currentSession.id);
            setExams(res.data || []);
        } catch {
            showToast("Failed to load exams", "error");
        } finally {
            setLoadingExams(false);
        }
    }

    /* ---------------- Handlers ---------------- */

    function onClassChange(e: ChangeEvent<HTMLSelectElement>) {
        const val = e.target.value;
        const parsedClassId = val ? Number(val) : "";
        setSelectedClass(parsedClassId);
        setExams([]);
        if (parsedClassId) {
            void loadExams(parsedClassId);
        }
    }

    /* ---------------- UI ---------------- */

    if (isSessionLoading) {
        return <div className="p-8 text-center text-gray-500">Initializing session...</div>;
    }

    if (!currentSession) {
        return (
            <div className="flex items-center justify-center min-h-[60vh]">
                <div className="text-center p-12 bg-white rounded-3xl border shadow-sm">
                    <span className="text-5xl mb-4 block">📅</span>
                    <h2 className="text-xl font-bold text-gray-800">No Academic Session Selected</h2>
                    <p className="text-gray-500 mt-2">Please select an academic session to manage exams.</p>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Exam Management</h1>
                    <p className="text-gray-500">Manage exam schedules and marks for <span className="text-blue-600 font-bold">{currentSession.name}</span>.</p>
                </div>

                {canCreateExam && (
                    <button
                        onClick={() => setShowAddModal(true)}
                        disabled={!selectedClass}
                        className={`
                            px-6 py-2.5 rounded-xl font-bold shadow-lg transition-all flex items-center gap-2
                            ${selectedClass
                                ? "bg-blue-600 text-white hover:bg-blue-700"
                                : "bg-gray-200 text-gray-400 cursor-not-allowed"}
                        `}
                    >
                        <span className="text-xl">+</span> Create Exam
                    </button>
                )}
            </div>

            <div className="bg-white p-6 border rounded-2xl shadow-sm flex flex-wrap gap-4 items-end">
                <div className="flex-1 min-w-[300px]">
                    <label className="block text-xs font-bold uppercase text-gray-400 mb-2 ml-1">Current Class</label>
                    <select
                        value={selectedClass}
                        onChange={onClassChange}
                        disabled={loadingClasses}
                        className="input-ref"
                    >
                        <option value="">Select Class</option>
                        {classes.map((c) => (
                            <option key={c.id} value={c.id}>
                                {c.name} {c.section}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {loadingExams ? (
                <div className="bg-white p-8 rounded-2xl border">
                    <TableSkeleton rows={5} cols={4} />
                </div>
            ) : selectedClass ? (
                <div className="bg-white border rounded-2xl shadow-sm overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-gray-50 text-gray-600 font-bold border-b">
                            <tr>
                                <th className="p-4 text-left">Exam Name</th>
                                <th className="p-4 text-center">Type</th>
                                <th className="p-4 text-center">Status</th>
                                <th className="p-4 text-center w-32">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {exams.map((exam) => (
                                <tr key={exam.id} className="hover:bg-gray-50/50 transition-colors">
                                    <td className="p-4 text-gray-800 font-bold">{exam.name}</td>
                                    <td className="p-4 text-center">
                                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-gray-100 text-gray-600 border border-gray-200">
                                            {exam.examType}
                                        </span>
                                    </td>
                                    <td className="p-4 text-center">
                                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${exam.status === 'PUBLISHED' ? "bg-blue-50 text-blue-600" :
                                            exam.status === 'LOCKED' ? "bg-gray-100 text-gray-600" :
                                                "bg-yellow-50 text-yellow-600"
                                            }`}>
                                            {exam.status}
                                        </span>
                                    </td>
                                    <td className="p-4 text-center">
                                        <button
                                            onClick={() => setSelectedExam(exam)}
                                            className="text-blue-600 hover:text-blue-700 font-bold text-xs"
                                        >
                                            Manage
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {exams.length === 0 && (
                                <tr>
                                    <td colSpan={4} className="p-20 text-center text-gray-400 italic bg-gray-50/30">
                                        <div className="flex flex-col items-center gap-3">
                                            <span className="text-3xl">📝</span>
                                            <div>
                                                <p className="font-bold text-gray-800">No Exams Scheduled</p>
                                                <p className="text-sm">No exam records found for this class in the current session.</p>
                                            </div>
                                            {canCreateExam && (
                                                <button
                                                    onClick={() => setShowAddModal(true)}
                                                    className="mt-2 bg-blue-50 text-blue-600 px-6 py-2 rounded-xl font-bold hover:bg-blue-100 transition-all border border-blue-200"
                                                >
                                                    Schedule First Exam →
                                                </button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            ) : (
                <div className="p-20 text-center bg-gray-50 rounded-2xl border border-dashed border-gray-300 text-gray-400 italic">
                    Please select a class to view and manage exams.
                </div>
            )}

            {/* Modals */}
            {showAddModal && (
                <ExamCreationModal
                    isOpen={showAddModal}
                    onClose={() => setShowAddModal(false)}
                    onSuccess={() => {
                        setShowAddModal(false);
                        if (selectedClass) loadExams(Number(selectedClass));
                    }}
                    classId={Number(selectedClass)}
                    sessionId={currentSession.id}
                />
            )}

            {selectedExam && (
                <ExamDetailView
                    exam={selectedExam}
                    classId={Number(selectedClass)}
                    onClose={() => setSelectedExam(null)}
                />
            )}
        </div>
    );
}
