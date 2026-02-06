"use client";

import { useEffect, useState } from "react";
import { schoolApi } from "@/lib/schoolApi";
import { studentApi } from "@/lib/studentApi";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";
import { TableSkeleton } from "@/components/ui/Skeleton";

/* ---------------- Types ---------------- */

type School = { id: number; name: string };
type SchoolClass = { id: number; name: string; section: string; session: string };
type Exam = { id: number; name: string; session: string; examType: string };
type Student = { id: number; firstName: string; lastName: string; admissionNumber: string };

/* ---------------- Page ---------------- */

export default function MarksheetsPage() {
    const { showToast } = useToast();

    /* ---------- State ---------- */
    const [schools, setSchools] = useState<School[]>([]);
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [exams, setExams] = useState<Exam[]>([]);

    const [selectedSchool, setSelectedSchool] = useState<number | "">("");
    const [selectedClass, setSelectedClass] = useState<number | "">("");
    const [selectedExam, setSelectedExam] = useState<number | "">("");

    const [students, setStudents] = useState<Student[]>([]);
    const [loading, setLoading] = useState({
        schools: true,
        classes: false,
        exams: false,
        students: false,
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
            setSchools(res.data.content || []);
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

            const res = await api.get(`/api/classes/by-school/${schoolId}`);
            setClasses(res.data.content || []);
        } catch {
            showToast("Failed to fetch classes for school", "error");
        } finally {
            setLoading(prev => ({ ...prev, classes: false }));
        }
    }

    async function loadExams(classId: number, session: string) {
        try {
            setLoading(prev => ({ ...prev, exams: true }));
            setExams([]);
            setSelectedExam("");
            setStudents([]);

            const res = await api.get(`/api/exams/by-class/${classId}?session=${session}`);
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
            const cls = classes.find(c => c.id === Number(classId));
            if (cls) {
                loadExams(cls.id, cls.session);
                loadStudents(cls.id);
            }
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
            <header>
                <h1 className="text-3xl font-bold text-gray-800">Academic Marks</h1>
                <p className="text-gray-500 mt-1">Generate and distribute official student report cards.</p>
            </header>

            {/* ---------------- Filters ---------------- */}
            <div className="bg-white p-6 border rounded-2xl shadow-sm grid grid-cols-1 md:grid-cols-3 gap-6 items-end">
                <div>
                    <label className="block text-[10px] font-black uppercase text-gray-400 mb-2 ml-1 tracking-widest">Institution Scope</label>
                    <select value={selectedSchool} onChange={onSchoolChange} className="input-ref font-bold">
                        <option value="">Select Target School</option>
                        {schools.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>

                <div>
                    <label className="block text-[10px] font-black uppercase text-gray-400 mb-2 ml-1 tracking-widest">Classroom / Year</label>
                    <select value={selectedClass} onChange={onClassChange} disabled={!selectedSchool} className="input-ref font-bold">
                        <option value="">Select Target Class</option>
                        {classes.map(c => <option key={c.id} value={c.id}>{c.name} {c.section} ({c.session})</option>)}
                    </select>
                </div>

                <div>
                    <label className="block text-[10px] font-black uppercase text-gray-400 mb-2 ml-1 tracking-widest">Exam Cycle</label>
                    <select
                        value={selectedExam}
                        onChange={(e) => setSelectedExam(Number(e.target.value))}
                        disabled={!selectedClass}
                        className="input-ref font-black text-blue-600"
                    >
                        <option value="">Select Exam Record</option>
                        {exams.map(ex => <option key={ex.id} value={ex.id}>{ex.name}</option>)}
                    </select>
                </div>
            </div>

            {/* ---------------- Content ---------------- */}
            {loading.students || loading.exams ? (
                <div className="bg-white p-8 rounded-2xl border shadow-sm">
                    <TableSkeleton rows={10} cols={3} />
                </div>
            ) : selectedExam ? (
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
                    {students.length === 0 && (
                        <div className="p-20 text-center text-gray-400 italic">No enrollment found for this query.</div>
                    )}
                </div>
            ) : (
                <div className="p-32 text-center bg-gray-50 rounded-3xl border border-dashed border-gray-300 text-gray-400 italic">
                    <div className="max-w-xs mx-auto space-y-4">
                        <div className="w-16 h-16 bg-white rounded-2xl border shadow-sm mx-auto flex items-center justify-center text-2xl">📋</div>
                        <p className="text-sm font-medium">Define your search parameters above to retrieve exam results and generate official marksheets.</p>
                    </div>
                </div>
            )}
        </div>
    );
}
