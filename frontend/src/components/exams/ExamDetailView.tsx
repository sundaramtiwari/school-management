"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { examApi } from "@/lib/examApi";
import { studentApi } from "@/lib/studentApi";
import { classSubjectApi } from "@/lib/classSubjectApi";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";
import Modal from "@/components/ui/Modal";
import MarksheetMVP from "./MarksheetMVP";
import { useAuth } from "@/context/AuthContext";

interface Exam {
    id: number;
    name: string;
    schoolId: number;
    sessionId: number;
    examType?: string;
    status: 'DRAFT' | 'PUBLISHED' | 'LOCKED';
}

interface ExamDetailViewProps {
    exam: Exam;
    classId: number;
    onClose: () => void;
}

interface ExamSubjectItem {
    id: number;
    subjectId: number;
    subjectName?: string;
    maxMarks: number;
}

interface StudentItem {
    id: number;
    firstName: string;
    lastName: string;
    admissionNumber?: string;
}

interface StudentMarkItem {
    studentId: number;
    examSubjectId: number;
    marksObtained: number;
}

interface ClassSubjectItem {
    id: number;
    subjectId: number;
    subjectName: string;
}

export default function ExamDetailView({ exam: initialExam, classId, onClose }: ExamDetailViewProps) {
    const { user } = useAuth();
    const { showToast } = useToast();
    const [exam, setExam] = useState<Exam>(initialExam);
    const [activeTab, setActiveTab] = useState<"subjects" | "marks" | "results">("subjects");

    const [subjects, setSubjects] = useState<ExamSubjectItem[]>([]);

    const [students, setStudents] = useState<StudentItem[]>([]);

    const [marksData, setMarksData] = useState<StudentMarkItem[]>([]);
    const [isSavingMarks, setIsSavingMarks] = useState(false);

    const [availableSubjects, setAvailableSubjects] = useState<ClassSubjectItem[]>([]);
    const [showAddSubject, setShowAddSubject] = useState(false);
    const [newSubject, setNewSubject] = useState({ subjectId: "", maxMarks: 100 });

    const [confirmAction, setConfirmAction] = useState<{ type: 'publish' | 'lock', isOpen: boolean } | null>(null);

    /* ---------------- Init ---------------- */

    useEffect(() => {
        setExam(initialExam);
    }, [initialExam]);

    const loadSubjects = useCallback(async () => {
        try {
            const res = await examApi.listSubjects(exam.id);
            setSubjects(res.data || []);
        } catch {
            showToast("Failed to load exam subjects", "error");
        }
    }, [exam.id, showToast]);

    const loadStudents = useCallback(async () => {
        try {
            const res = await studentApi.byClass(classId, 0, 100);
            setStudents(res.data.content || []);
        } catch {
            showToast("Failed to load students", "error");
        }
    }, [classId, showToast]);

    const loadAvailableSubjects = useCallback(async () => {
        try {
            if (user?.role === "TEACHER") {
                const res = await api.get<{ id: number; name: string }[]>(
                    `/api/teacher-assignments/my-subjects?sessionId=${exam.sessionId}&classId=${classId}`
                );
                const mapped = (res.data || []).map((s) => ({
                    id: s.id,
                    subjectId: s.id,
                    subjectName: s.name,
                }));
                setAvailableSubjects(mapped);
            } else {
                const res = await classSubjectApi.getByClass(classId, 0, 100);
                setAvailableSubjects(res.content || []);
            }
        } catch {
            showToast("Failed to load available subjects", "error");
        }
    }, [classId, showToast, user?.role, exam.sessionId]);

    const loadExistingMarks = useCallback(async () => {
        try {
            const res = await examApi.listMarks(exam.id);
            setMarksData(res.data || []);
        } catch {
            showToast("Failed to load existing marks", "error");
        }
    }, [exam.id, showToast]);

    useEffect(() => {
        if (exam?.id) {
            void loadSubjects();
            void loadStudents();
            void loadAvailableSubjects();
            void loadExistingMarks();
        }
    }, [exam?.id, loadSubjects, loadStudents, loadAvailableSubjects, loadExistingMarks]);

    /* ---------------- Handlers ---------------- */

    async function handleAddSubject() {
        if (exam.status !== 'DRAFT') {
            showToast("Cannot add subjects to a published or locked exam.", "error");
            return;
        }
        if (!newSubject.subjectId || !newSubject.maxMarks) {
            showToast("Please fill all fields", "warning");
            return;
        }
        try {
            await examApi.addSubject({
                examId: exam.id,
                subjectId: Number(newSubject.subjectId),
                maxMarks: Number(newSubject.maxMarks),
            });
            showToast("Subject added successfully", "success");
            setShowAddSubject(false);
            void loadSubjects();
        } catch {
            showToast("Failed to add subject", "error");
        }
    }

    async function handleBulkSave(editedMarks: StudentMarkItem[]) {
        if (exam.status !== 'DRAFT') {
            showToast("Marks can only be processed when exam is in DRAFT status.", "error");
            return;
        }
        try {
            setIsSavingMarks(true);
            await examApi.saveMarksBulk(exam.id, { marks: editedMarks });
            showToast("Marks saved successfully", "success");
            void loadExistingMarks();
        } catch {
            showToast("Failed to save marks", "error");
        } finally {
            setIsSavingMarks(false);
        }
    }

    async function handlePublish() {
        try {
            const res = await examApi.publish(exam.id);
            setExam(res.data);
            setConfirmAction(null);
            showToast("Exam published successfully!", "success");
        } catch (e: unknown) {
            const msg = typeof e === "object" && e !== null && "message" in e
                ? String((e as { message?: string }).message || "Failed to publish exam")
                : "Failed to publish exam";
            showToast(msg, "error");
        }
    }

    async function handleLock() {
        try {
            const res = await examApi.lock(exam.id);
            setExam(res.data);
            setConfirmAction(null);
            showToast("Exam locked successfully!", "success");
        } catch (e: unknown) {
            const msg = typeof e === "object" && e !== null && "message" in e
                ? String((e as { message?: string }).message || "Failed to lock exam")
                : "Failed to lock exam";
            showToast(msg, "error");
        }
    }

    function getStatusBadge(status: string) {
        switch (status) {
            case 'DRAFT': return <span className="px-2 py-1 text-xs font-bold rounded bg-yellow-100 text-yellow-800">DRAFT</span>;
            case 'PUBLISHED': return <span className="px-2 py-1 text-xs font-bold rounded bg-blue-100 text-blue-800">PUBLISHED</span>;
            case 'LOCKED': return <span className="px-2 py-1 text-xs font-bold rounded bg-gray-100 text-gray-800">LOCKED</span>;
            default: return <span className="px-2 py-1 text-xs font-bold rounded bg-gray-100 text-gray-800">{status}</span>;
        }
    }

    /* ---------------- UI ---------------- */

    return (
        <Modal
            isOpen={!!exam}
            onClose={onClose}
            title={
                <div className="flex items-center gap-3">
                    <span>Exam: {exam.name}</span>
                    {getStatusBadge(exam.status)}
                </div>
            }
            maxWidth="max-w-6xl"
        >
            <div className="flex flex-col gap-6">
                {/* Actions */}
                <div className="flex justify-end gap-2">
                    {exam.status === 'DRAFT' && (
                        <button
                            onClick={() => setConfirmAction({ type: 'publish', isOpen: true })}
                            className="bg-blue-600 text-white px-4 py-2 rounded text-sm font-bold hover:bg-blue-700"
                        >
                            Publish Exam
                        </button>
                    )}
                    {exam.status === 'PUBLISHED' && (
                        <button
                            onClick={() => setConfirmAction({ type: 'lock', isOpen: true })}
                            className="bg-gray-800 text-white px-4 py-2 rounded text-sm font-bold hover:bg-gray-900"
                        >
                            Lock Exam
                        </button>
                    )}
                </div>

                {/* Tabs */}
                <div className="flex gap-4 border-b">
                    {(["subjects", "marks", "results"] as const).map((tab) => (
                        <button
                            key={tab}
                            onClick={() => setActiveTab(tab)}
                            className={`px-4 py-2 text-sm font-bold capitalize transition-all border-b-2 ${activeTab === tab
                                ? "border-blue-600 text-blue-600"
                                : "border-transparent text-gray-400 hover:text-gray-600"
                                }`}
                        >
                            {tab}
                        </button>
                    ))}
                </div>

                {/* Tab Content */}
                <div className="min-h-[400px]">
                    {activeTab === "subjects" && (
                        <div className="space-y-4">
                            <div className="flex justify-between items-center">
                                <h3 className="font-bold text-gray-700">Exam Subjects</h3>
                                {exam.status === 'DRAFT' && (
                                    <button
                                        onClick={() => setShowAddSubject(true)}
                                        className="bg-blue-600 text-white px-4 py-1.5 rounded-lg text-xs font-bold"
                                    >
                                        + Add Subject
                                    </button>
                                )}
                            </div>

                            <div className="bg-gray-50 rounded-xl border overflow-hidden">
                                <table className="w-full text-sm">
                                    <thead className="bg-gray-100 border-b">
                                        <tr>
                                            <th className="p-3 text-left">Subject</th>
                                            <th className="p-3 text-center">Max Marks</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y">
                                        {subjects.map((s) => (
                                            <tr key={s.id}>
                                                <td className="p-3 font-semibold">{s.subjectName || `Subject #${s.subjectId}`}</td>
                                                <td className="p-3 text-center">{s.maxMarks}</td>
                                            </tr>
                                        ))}
                                        {subjects.length === 0 && (
                                            <tr>
                                                <td colSpan={2} className="p-10 text-center text-gray-400 italic">No subjects added yet.</td>
                                            </tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    )}

                    {activeTab === "marks" && (
                        <MarksEntryTable
                            students={students}
                            subjects={subjects}
                            initialMarks={marksData}
                            onSave={handleBulkSave}
                            isSaving={isSavingMarks}
                            readOnly={exam.status !== 'DRAFT'}
                        />
                    )}

                    {activeTab === "results" && (
                        <MarksheetMVP
                            students={students}
                            subjects={subjects}
                            marks={marksData}
                        />
                    )}
                </div>
            </div>

            {/* Add Subject Modal */}
            {showAddSubject && (
                <Modal
                    isOpen={showAddSubject}
                    onClose={() => setShowAddSubject(false)}
                    title="Add Subject to Exam"
                    footer={
                        <div className="flex gap-2 text-sm">
                            <button
                                onClick={() => setShowAddSubject(false)}
                                className="px-4 py-2 border rounded-lg"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleAddSubject}
                                className="px-4 py-2 bg-blue-600 text-white font-bold rounded-lg"
                            >
                                Add Subject
                            </button>
                        </div>
                    }
                >
                    <div className="space-y-4">
                        <div className="space-y-1">
                            <label className="text-xs font-bold text-gray-400 uppercase">Select Subject</label>
                            <select
                                value={newSubject.subjectId}
                                onChange={(e) => setNewSubject({ ...newSubject, subjectId: e.target.value })}
                                className="input-ref"
                            >
                                <option value="">Select Subject</option>
                                {availableSubjects.map((s) => (
                                    <option key={s.id} value={s.subjectId}>{s.subjectName}</option>
                                ))}
                            </select>
                        </div>
                        <div className="space-y-1">
                            <label className="text-xs font-bold text-gray-400 uppercase">Max Marks</label>
                            <input
                                type="number"
                                value={newSubject.maxMarks}
                                onChange={(e) => setNewSubject({ ...newSubject, maxMarks: Number(e.target.value) })}
                                className="input-ref"
                            />
                        </div>
                    </div>
                </Modal>
            )}

            {/* Confirmation Dialog */}
            {confirmAction && (
                <Modal
                    isOpen={confirmAction.isOpen}
                    onClose={() => setConfirmAction(null)}
                    title={`Confirm ${confirmAction.type === 'publish' ? 'Publish' : 'Lock'} Exam`}
                    footer={
                        <div className="flex gap-2 text-sm">
                            <button
                                onClick={() => setConfirmAction(null)}
                                className="px-4 py-2 border rounded-lg"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={confirmAction.type === 'publish' ? handlePublish : handleLock}
                                className={`px-4 py-2 text-white font-bold rounded-lg ${confirmAction.type === 'publish' ? 'bg-blue-600' : 'bg-gray-800'
                                    }`}
                            >
                                Confirm {confirmAction.type === 'publish' ? 'Publish' : 'Lock'}
                            </button>
                        </div>
                    }
                >
                    <p className="text-gray-600">
                        {confirmAction.type === 'publish'
                            ? "Are you sure you want to publish this exam? This will make it visible to students and parents."
                            : "Are you sure you want to lock this exam? This action cannot be undone and no further changes will be allowed."}
                    </p>
                </Modal>
            )}
        </Modal>
    );
}

/* ---------------- Marks Entry Component ---------------- */

interface MarksEntryTableProps {
    students: StudentItem[];
    subjects: ExamSubjectItem[];
    initialMarks: StudentMarkItem[];
    onSave: (items: StudentMarkItem[]) => void;
    isSaving: boolean;
    readOnly: boolean;
}

function MarksEntryTable({ students, subjects, initialMarks, onSave, isSaving, readOnly }: MarksEntryTableProps) {
    const [editedMarks, setEditedMarks] = useState<Record<string, number>>({});
    const initialMarksMap = useMemo(() => {
        const map: Record<string, number> = {};
        initialMarks.forEach((m) => {
            map[`${m.studentId}-${m.examSubjectId}`] = m.marksObtained;
        });
        return map;
    }, [initialMarks]);
    const localMarks = useMemo(
        () => ({ ...initialMarksMap, ...editedMarks }),
        [initialMarksMap, editedMarks]
    );

    function handleMarkChange(studentId: number, examSubjectId: number, val: string) {
        if (readOnly) return;
        const num = parseInt(val) || 0;
        setEditedMarks((prev) => ({
            ...prev,
            [`${studentId}-${examSubjectId}`]: num
        }));
    }

    function doSave() {
        if (readOnly) return;
        const payload = Object.entries(localMarks).map(([key, val]) => {
            const [studentId, examSubjectId] = key.split("-");
            return {
                studentId: Number(studentId),
                examSubjectId: Number(examSubjectId),
                marksObtained: val
            };
        });
        onSave(payload);
    }

    if (subjects.length === 0) {
        return <div className="p-20 text-center text-gray-400">Please add subjects first to enter marks.</div>;
    }

    return (
        <div className="space-y-4">
            <div className="flex justify-between items-center">
                <h3 className="font-bold text-gray-700">Enter Marks</h3>
                {!readOnly && (
                    <button
                        onClick={doSave}
                        disabled={isSaving}
                        className="bg-blue-600 text-white px-6 py-2 rounded-xl font-bold text-sm shadow-md hover:bg-blue-700 disabled:bg-gray-400"
                    >
                        {isSaving ? "Saving..." : "Save All Marks"}
                    </button>
                )}
                {readOnly && (
                    <span className="text-sm text-gray-500 italic bg-gray-100 px-3 py-1 rounded">
                        Read Only (Exam is not DRAFT)
                    </span>
                )}
            </div>

            <div className="overflow-x-auto bg-white border rounded-xl shadow-sm">
                <table className="w-full text-sm">
                    <thead className="bg-gray-50 border-b">
                        <tr>
                            <th className="p-4 text-left border-r sticky left-0 bg-gray-50 z-10 w-48">Student Name</th>
                            {subjects.map((s) => (
                                <th key={s.id} className="p-4 text-center border-r min-w-[120px]">
                                    {s.subjectName || `Subject #${s.subjectId}`}
                                    <div className="text-[10px] text-gray-400 font-normal mt-1">(Max: {s.maxMarks})</div>
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody className="divide-y">
                        {students.map((st) => (
                            <tr key={st.id} className="hover:bg-gray-50/50">
                                <td className="p-4 font-bold text-gray-800 border-r sticky left-0 bg-white z-10 shadow-[2px_0_5px_rgba(0,0,0,0.05)]">
                                    {st.firstName} {st.lastName}
                                </td>
                                {subjects.map((s) => {
                                    const key = `${st.id}-${s.id}`;
                                    return (
                                        <td key={s.id} className="p-4 text-center border-r">
                                            <input
                                                type="number"
                                                min="0"
                                                max={s.maxMarks}
                                                value={localMarks[key] !== undefined ? localMarks[key] : ""}
                                                onChange={(e) => handleMarkChange(st.id, s.id, e.target.value)}
                                                disabled={readOnly}
                                                className={`w-20 px-2 py-1 text-center border rounded font-mono font-bold focus:ring-2 focus:ring-blue-500 outline-none ${readOnly ? "bg-gray-100 text-gray-500 cursor-not-allowed" : ""
                                                    }`}
                                            />
                                        </td>
                                    );
                                })}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
