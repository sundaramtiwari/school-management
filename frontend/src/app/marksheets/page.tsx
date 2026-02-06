"use client";

import { useEffect, useState } from "react";
import { schoolApi } from "@/lib/schoolApi";
import { studentApi } from "@/lib/studentApi";
import { api } from "@/lib/api";

/* ---------------- Types ---------------- */

type School = {
    id: number;
    name: string;
};

type SchoolClass = {
    id: number;
    name: string;
    section: string;
    session: string;
};

type Exam = {
    id: number;
    name: string;
    session: string;
    examType: string;
};

type Student = {
    id: number;
    firstName: string;
    lastName: string;
    admissionNumber: string;
};

/* ---------------- Page ---------------- */

export default function MarksheetsPage() {

    /* ---------- Filters ---------- */

    const [schools, setSchools] = useState<School[]>([]);
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [exams, setExams] = useState<Exam[]>([]);

    const [selectedSchool, setSelectedSchool] = useState<number | "">("");
    const [selectedClass, setSelectedClass] = useState<number | "">("");
    const [selectedExam, setSelectedExam] = useState<number | "">("");

    /* ---------- Results ---------- */

    const [students, setStudents] = useState<Student[]>([]);
    const [loading, setLoading] = useState({
        schools: true,
        classes: false,
        exams: false,
        students: false,
    });

    const [error, setError] = useState("");

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
            setError("Failed to load schools");
        } finally {
            setLoading(prev => ({ ...prev, schools: false }));
        }
    }

    /* ---------------- Load Classes ---------------- */

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
            setError("Failed to load classes");
        } finally {
            setLoading(prev => ({ ...prev, classes: false }));
        }
    }

    /* ---------------- Load Exams ---------------- */

    async function loadExams(classId: number, session: string) {
        try {
            setLoading(prev => ({ ...prev, exams: true }));
            setExams([]);
            setSelectedExam("");
            setStudents([]);

            const res = await api.get(`/api/exams/by-class/${classId}?session=${session}`);
            setExams(res.data || []);
        } catch {
            setError("Failed to load exams");
        } finally {
            setLoading(prev => ({ ...prev, exams: false }));
        }
    }

    /* ---------------- Load Students ---------------- */

    async function loadStudents(classId: number) {
        try {
            setLoading(prev => ({ ...prev, students: true }));
            const res = await studentApi.byClass(classId, 0, 100);
            setStudents(res.data.content || []);
        } catch {
            setError("Failed to load students");
        } finally {
            setLoading(prev => ({ ...prev, students: false }));
        }
    }

    /* ---------------- Handlers ---------------- */

    function onSchoolChange(e: any) {
        const value = e.target.value;
        setSelectedSchool(value);
        if (value) loadClasses(Number(value));
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

    /* ---------------- Download ---------------- */

    async function downloadMarksheet(studentId: number) {
        if (!selectedExam) return;

        try {
            const res = await api.get(`/api/marksheets/exam/${selectedExam}/student/${studentId}/pdf`, {
                responseType: 'blob'
            });

            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `marksheet_${studentId}.pdf`);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch {
            alert("Failed to download marksheet");
        }
    }

    /* ---------------- UI ---------------- */

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-gray-800">Exam Results & Marksheets</h1>
            </div>

            {/* ---------------- Filters ---------------- */}
            <div className="bg-white p-6 border rounded-xl shadow-sm flex flex-wrap gap-4 items-end">

                <div className="flex-1 min-w-[200px]">
                    <label className="block text-xs font-semibold uppercase text-gray-500 mb-2">School</label>
                    <select
                        value={selectedSchool}
                        onChange={onSchoolChange}
                        className="w-full border-gray-200 rounded-lg px-4 py-2 focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="">Select School</option>
                        {schools.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                </div>

                <div className="flex-1 min-w-[200px]">
                    <label className="block text-xs font-semibold uppercase text-gray-500 mb-2">Class</label>
                    <select
                        value={selectedClass}
                        onChange={onClassChange}
                        disabled={!selectedSchool || loading.classes}
                        className="w-full border-gray-200 rounded-lg px-4 py-2 disabled:bg-gray-50 disabled:text-gray-400"
                    >
                        <option value="">Select Class</option>
                        {classes.map(c => (
                            <option key={c.id} value={c.id}>
                                {c.name} {c.section} ({c.session})
                            </option>
                        ))}
                    </select>
                </div>

                <div className="flex-1 min-w-[200px]">
                    <label className="block text-xs font-semibold uppercase text-gray-500 mb-2">Exam</label>
                    <select
                        value={selectedExam}
                        onChange={(e) => setSelectedExam(Number(e.target.value))}
                        disabled={!selectedClass || loading.exams}
                        className="w-full border-gray-200 rounded-lg px-4 py-2 disabled:bg-gray-50"
                    >
                        <option value="">Select Exam</option>
                        {exams.map(ex => <option key={ex.id} value={ex.id}>{ex.name}</option>)}
                    </select>
                </div>
            </div>

            {error && <div className="bg-red-50 text-red-600 p-4 rounded-lg border border-red-100">{error}</div>}

            {/* ---------------- Students Table ---------------- */}
            {selectedExam ? (
                <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-gray-50 text-gray-600 font-medium">
                            <tr>
                                <th className="p-4 text-left">Admission No</th>
                                <th className="p-4 text-left">Student Name</th>
                                <th className="p-4 text-center">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {students.map((s) => (
                                <tr key={s.id} className="hover:bg-gray-50 transition-colors">
                                    <td className="p-4 font-medium text-gray-700">{s.admissionNumber}</td>
                                    <td className="p-4 text-gray-600">{s.firstName} {s.lastName}</td>
                                    <td className="p-4 text-center">
                                        <button
                                            onClick={() => downloadMarksheet(s.id)}
                                            className="bg-blue-50 text-blue-600 hover:bg-blue-600 hover:text-white px-4 py-1.5 rounded-lg transition-all font-medium border border-blue-100"
                                        >
                                            Download Marksheet
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {students.length === 0 && (
                                <tr>
                                    <td colSpan={3} className="p-12 text-center text-gray-400 italic">
                                        No students found in this class
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            ) : (
                selectedClass && !loading.exams && (
                    <div className="text-center p-12 bg-blue-50 rounded-xl border border-blue-100 text-blue-500">
                        Please select an exam to view students and download marksheets
                    </div>
                )
            )}
        </div>
    );
}
