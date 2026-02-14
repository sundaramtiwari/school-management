"use client";

import { useEffect, useState } from "react";
import { schoolApi } from "@/lib/schoolApi";
import { studentApi } from "@/lib/studentApi";
import { api } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/components/ui/Toast";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { useSession } from "@/context/SessionContext";

/* ---------------- Types ---------------- */

type School = { id: number; name: string };
type SchoolClass = { id: number; name: string; section: string; sessionId: number };
type Exam = { id: number; name: string; sessionId: number; examType: string };
type Student = { id: number; firstName: string; lastName: string; admissionNumber: string };
type ExamSubject = { id: number; examId: number; subjectId: number; maxMarks: number; subjectName?: string };
type StudentMark = { studentId: number; examSubjectId: number; marksObtained: number };

/* ---------------- Page ---------------- */

export default function MarksheetsPage() {
    const { user } = useAuth();
    const { showToast } = useToast();
    const { currentSession } = useSession();

    /* ---------- State ---------- */
    const [schools, setSchools] = useState<School[]>([]);
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [exams, setExams] = useState<Exam[]>([]);

    const [selectedSchool, setSelectedSchool] = useState<number | "">("");
    const [selectedClass, setSelectedClass] = useState<number | "">("");
    const [selectedExam, setSelectedExam] = useState<number | "">("");

    const [mode, setMode] = useState<"view" | "enter">("view");
    const [students, setStudents] = useState<Student[]>([]);
    const [examSubjects, setExamSubjects] = useState<ExamSubject[]>([]);
    const [marksMatrix, setMarksMatrix] = useState<Record<string, number>>({});

    const [loading, setLoading] = useState({
        schools: true,
        classes: false,
        exams: false,
        students: false,
        examData: false,
        saving: false,
        downloadingId: null as number | null
    });

    /* ---------------- Init ---------------- */

    useEffect(() => {
        loadSchools();
    }, []);

    async function loadSchools() {
        try {
            setLoading(prev => ({ ...prev, schools: true }));
            const res = await schoolApi.list(0, 100);
            const loadedSchools = res.data.content || [];
            setSchools(loadedSchools);

            if (user?.schoolId) {
                const userSchool = loadedSchools.find((s: School) => s.id === user.schoolId);
                if (userSchool) {
                    setSelectedSchool(userSchool.id);
                    loadClasses(userSchool.id);
                }
            } else if (loadedSchools.length === 1) {
                setSelectedSchool(loadedSchools[0].id);
                loadClasses(loadedSchools[0].id);
            }
        } catch {
            showToast("Failed to pull school list", "error");
        } finally {
            setLoading(prev => ({ ...prev, schools: false }));
        }
    }

    async function loadClasses(schoolId: number) {
        try {
            setLoading(prev => ({ ...prev, classes: true }));
            setClasses([]);
            setSelectedClass("");
            setExams([]);
            setSelectedExam("");
            setStudents([]);

            const res = await api.get("/api/classes/mine?size=100");
            setClasses(res.data.content || []);
        } catch {
            showToast("Failed to fetch classes for school", "error");
        } finally {
            setLoading(prev => ({ ...prev, classes: false }));
        }
    }

    async function loadExams(classId: number) {
        if (!currentSession) return;
        try {
            setLoading(prev => ({ ...prev, exams: true }));
            setExams([]);
            setSelectedExam("");
            setStudents([]);

            const res = await api.get(`/api/exams/by-class/${classId}?sessionId=${currentSession.id}`);
            setExams(res.data || []);
        } catch {
            showToast("No active exams found for this class", "warning");
        } finally {
            setLoading(prev => ({ ...prev, exams: false }));
        }
    }

    async function loadStudents(classId: number) {
        try {
            setLoading(prev => ({ ...prev, students: true }));
            const res = await studentApi.byClass(classId, 0, 100);
            setStudents(res.data.content || []);
        } catch {
            showToast("Failed to load student roster", "error");
        } finally {
            setLoading(prev => ({ ...prev, students: false }));
        }
    }

    async function loadExamData(examId: number) {
        try {
            setLoading(prev => ({ ...prev, examData: true }));

            // 1. Load subjects
            const subjectsRes = await api.get(`/api/exam-subjects/by-exam/${examId}`);
            const subjects: ExamSubject[] = subjectsRes.data || [];

            // 2. Load existing marks
            const marksRes = await api.get(`/api/marks/exam/${examId}`);
            const marks: StudentMark[] = marksRes.data || [];

            // 3. Populate matrix
            const matrix: Record<string, number> = {};
            marks.forEach(m => {
                matrix[`${m.studentId}-${m.examSubjectId}`] = m.marksObtained;
            });

            setExamSubjects(subjects);
            setMarksMatrix(matrix);
        } catch {
            showToast("Failed to load exam subjects or marks", "error");
        } finally {
            setLoading(prev => ({ ...prev, examData: false }));
        }
    }

    /* ---------------- Handlers ---------------- */

    function onSchoolChange(e: any) {
        const val = e.target.value;
        setSelectedSchool(val);
        if (val) loadClasses(Number(val));
    }

    function onClassChange(e: any) {
        const classId = e.target.value;
        setSelectedClass(classId);
        if (classId) {
            loadExams(Number(classId));
            loadStudents(Number(classId));
        }
    }

    function onExamChange(e: any) {
        const examId = Number(e.target.value);
        setSelectedExam(examId);
        if (examId && mode === "enter") {
            loadExamData(examId);
        }
    }

    function onModeToggle(newMode: "view" | "enter") {
        setMode(newMode);
        if (newMode === "enter" && selectedExam) {
            loadExamData(Number(selectedExam));
        }
    }

    function handleMarkChange(studentId: number, subjectId: number, value: string) {
        const numVal = value === "" ? 0 : Number(value);
        setMarksMatrix(prev => ({
            ...prev,
            [`${studentId}-${subjectId}`]: numVal
        }));
    }

    async function saveAllMarks() {
        if (!selectedExam) return;
        try {
            setLoading(prev => ({ ...prev, saving: true }));

            const markItems = Object.entries(marksMatrix).map(([key, val]) => {
                const [studentId, examSubjectId] = key.split("-").map(Number);
                return { studentId, examSubjectId, marksObtained: val };
            });

            await api.post(`/api/exams/${selectedExam}/marks/bulk`, { marks: markItems });
            showToast("All marks saved successfully", "success");
        } catch (err: any) {
            showToast(err.response?.data?.message || "Failed to save marks", "error");
        } finally {
            setLoading(prev => ({ ...prev, saving: false }));
        }
    }

    async function downloadMarksheet(studentId: number) {
        if (!selectedExam) return;
        try {
            setLoading(prev => ({ ...prev, downloadingId: studentId }));
            const res = await api.get(`/api/marksheets/exam/${selectedExam}/student/${studentId}/pdf`, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `report_card_${studentId}.pdf`);
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            showToast("Report card generated successfully", "success");
        } catch {
            showToast("Failed to generate PDF report", "error");
        } finally {
            setLoading(prev => ({ ...prev, downloadingId: null }));
        }
    }

    return (
        <div className="space-y-6">
            <header className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800 tracking-tight">Academic Marks</h1>
                    <p className="text-gray-500 mt-1 font-medium">Manage student performance and distribute report cards for <span className="text-blue-600 font-bold">{currentSession?.name || "current session"}</span>.</p>
                </div>

                <div className="flex bg-gray-100 p-1 rounded-xl w-fit">
                    <button
                        onClick={() => onModeToggle("view")}
                        className={`px-6 py-2 rounded-lg text-sm font-black transition-all ${mode === "view" ? "bg-white shadow-sm text-blue-600" : "text-gray-500 hover:text-gray-700"}`}
                    >
                        VIEW REPORTS
                    </button>
                    <button
                        onClick={() => onModeToggle("enter")}
                        className={`px-6 py-2 rounded-lg text-sm font-black transition-all ${mode === "enter" ? "bg-white shadow-sm text-blue-600" : "text-gray-500 hover:text-gray-700"}`}
                    >
                        ENTER MARKS
                    </button>
                </div>
            </header>

            {/* ---------------- Filters ---------------- */}
            <div className="bg-white p-6 border rounded-2xl shadow-sm grid grid-cols-1 md:grid-cols-3 gap-6 items-end">
                <div>
                    <label className="block text-[10px] font-black uppercase text-gray-400 mb-2 ml-1 tracking-widest">Institution Scope</label>
                    <select
                        value={selectedSchool}
                        onChange={onSchoolChange}
                        className="input-ref font-bold"
                        disabled={!!user?.schoolId}
                    >
                        <option value="">Select Target School</option>
                        {schools.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>

                <div>
                    <label className="block text-[10px] font-black uppercase text-gray-400 mb-2 ml-1 tracking-widest">Classroom / Year</label>
                    <select value={selectedClass} onChange={onClassChange} disabled={!selectedSchool} className="input-ref font-bold">
                        <option value="">Select Target Class</option>
                        {classes.map(c => <option key={c.id} value={c.id}>{c.name} {c.section}</option>)}
                    </select>
                </div>

                <div>
                    <label className="block text-[10px] font-black uppercase text-gray-400 mb-2 ml-1 tracking-widest">Exam Cycle</label>
                    <select
                        value={selectedExam}
                        onChange={onExamChange}
                        disabled={!selectedClass}
                        className="input-ref font-black text-blue-600"
                    >
                        <option value="">Select Exam Record</option>
                        {exams.map(ex => <option key={ex.id} value={ex.id}>{ex.name}</option>)}
                    </select>
                </div>
            </div>

            {/* ---------------- Content ---------------- */}
            {loading.students || loading.exams || loading.examData ? (
                <div className="bg-white p-8 rounded-2xl border shadow-sm">
                    <TableSkeleton rows={10} cols={mode === "view" ? 3 : 5} />
                </div>
            ) : selectedExam ? (
                mode === "view" ? (
                    <div className="bg-white border rounded-2xl shadow-sm overflow-hidden animate-in fade-in slide-in-from-bottom-2">
                        <table className="w-full text-sm">
                            <thead className="bg-gray-50 text-gray-400 font-black text-[10px] uppercase tracking-widest border-b">
                                <tr>
                                    <th className="p-4 text-left w-1/4">Admission No</th>
                                    <th className="p-4 text-left">Full Name</th>
                                    <th className="p-4 text-center w-48">Download Report</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {students.map((s) => (
                                    <tr key={s.id} className="hover:bg-gray-50/50 transition-colors">
                                        <td className="p-4 font-mono font-medium text-gray-400">{s.admissionNumber}</td>
                                        <td className="p-4 font-black text-gray-800">{s.firstName} {s.lastName}</td>
                                        <td className="p-4 text-center">
                                            <button
                                                onClick={() => downloadMarksheet(s.id)}
                                                disabled={loading.downloadingId === s.id}
                                                className="w-full bg-blue-50 text-blue-600 hover:bg-blue-600 hover:text-white px-4 py-2 rounded-xl transition-all font-black text-[10px] uppercase border border-blue-100 disabled:opacity-50 tracking-tighter"
                                            >
                                                {loading.downloadingId === s.id ? "Generating..." : "Get PDF Marksheet"}
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div className="space-y-4 animate-in fade-in slide-in-from-bottom-2">
                        <div className="bg-white border rounded-2xl shadow-sm overflow-x-auto">
                            <table className="w-full text-sm">
                                <thead className="bg-gray-50 text-gray-400 font-black text-[10px] uppercase tracking-widest border-b">
                                    <tr>
                                        <th className="p-4 text-left sticky left-0 bg-gray-50 z-10">Student</th>
                                        {examSubjects.map(sub => (
                                            <th key={sub.id} className="p-4 text-center">
                                                {sub.subjectName || `Subject ${sub.subjectId}`}
                                                <div className="text-[9px] text-gray-300">Max: {sub.maxMarks}</div>
                                            </th>
                                        ))}
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {students.map(s => (
                                        <tr key={s.id} className="hover:bg-gray-50/30 transition-colors">
                                            <td className="p-4 font-black text-gray-800 sticky left-0 bg-white border-r">
                                                {s.firstName} {s.lastName}
                                                <div className="text-[10px] text-gray-400 font-mono font-normal uppercase">{s.admissionNumber}</div>
                                            </td>
                                            {examSubjects.map(sub => {
                                                const key = `${s.id}-${sub.id}`;
                                                return (
                                                    <td key={sub.id} className="p-2 text-center">
                                                        <input
                                                            type="number"
                                                            value={marksMatrix[key] ?? ""}
                                                            onChange={(e) => handleMarkChange(s.id, sub.id, e.target.value)}
                                                            max={sub.maxMarks}
                                                            min={0}
                                                            placeholder="0"
                                                            className="w-20 p-2 text-center font-bold text-gray-700 bg-gray-50 border border-gray-100 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-100 transition-all"
                                                        />
                                                    </td>
                                                );
                                            })}
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        <div className="flex justify-end pt-2">
                            <button
                                onClick={saveAllMarks}
                                disabled={loading.saving || students.length === 0}
                                className="bg-blue-600 text-white hover:bg-blue-700 px-10 py-4 rounded-2xl shadow-lg shadow-blue-200 transition-all font-black text-xs uppercase tracking-widest disabled:opacity-50"
                            >
                                {loading.saving ? "Saving Data..." : "Finalize & Save All Marks"}
                            </button>
                        </div>
                    </div>
                )
            ) : (
                <div className="p-32 text-center bg-gray-50 rounded-3xl border border-dashed border-gray-300 text-gray-400 italic">
                    <div className="max-w-xs mx-auto space-y-4">
                        <div className="w-16 h-16 bg-white rounded-2xl border shadow-sm mx-auto flex items-center justify-center text-2xl">📋</div>
                        <p className="text-sm font-medium">Define your search parameters above to retrieve exam records and {mode === "view" ? "generate marksheets" : "input student marks"}.</p>
                    </div>
                </div>
            )}
        </div>
    );
}
