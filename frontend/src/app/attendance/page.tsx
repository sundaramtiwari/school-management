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

type Student = {
    id: number;
    firstName: string;
    lastName: string;
    admissionNumber: string;
};

type AttendanceStatus = "PRESENT" | "ABSENT" | "LATE" | "HALF_DAY";

type AttendanceRecord = {
    studentId: number;
    status: AttendanceStatus;
    remarks?: string;
};

/* ---------------- Page ---------------- */

export default function AttendancePage() {

    /* ---------- Filters ---------- */

    const [schools, setSchools] = useState<School[]>([]);
    const [classes, setClasses] = useState<SchoolClass[]>([]);

    const [selectedSchool, setSelectedSchool] = useState<number | "">("");
    const [selectedClass, setSelectedClass] = useState<number | "">("");
    const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().split('T')[0]);

    /* ---------- Data ---------- */

    const [students, setStudents] = useState<Student[]>([]);
    const [attendanceMap, setAttendanceMap] = useState<Record<number, AttendanceStatus>>({});

    const [loading, setLoading] = useState({
        schools: true,
        classes: false,
        students: false,
        saving: false,
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
            setStudents([]);
            setAttendanceMap({});

            const res = await api.get(`/api/classes/by-school/${schoolId}`);
            setClasses(res.data.content || []);
        } catch {
            setError("Failed to load classes");
        } finally {
            setLoading(prev => ({ ...prev, classes: false }));
        }
    }

    /* ---------------- Load Students & Attendance ---------------- */

    async function loadStudentsAndAttendance(classId: number, date: string) {
        try {
            setLoading(prev => ({ ...prev, students: true }));
            setError("");

            const cls = classes.find(c => c.id === classId);
            if (!cls) return;

            // 1. Load Students
            const studentRes = await studentApi.byClass(classId, 0, 100);
            const studentList = studentRes.data.content || [];
            setStudents(studentList);

            // 2. Load Existing Attendance
            const attendanceRes = await api.get(`/api/attendance/class/${classId}?session=${cls.session}&date=${date}`);
            const existingAttendance: any[] = attendanceRes.data || [];

            const map: Record<number, AttendanceStatus> = {};

            // Default everyone to PRESENT if no attendance recorded yet
            studentList.forEach((s: Student) => {
                map[s.id] = "PRESENT";
            });

            // Override with existing records
            existingAttendance.forEach(record => {
                map[record.studentId] = record.status;
            });

            setAttendanceMap(map);

        } catch {
            setError("Failed to load students or attendance records");
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
        if (classId && selectedDate) {
            loadStudentsAndAttendance(Number(classId), selectedDate);
        }
    }

    function onDateChange(e: any) {
        const date = e.target.value;
        setSelectedDate(date);
        if (selectedClass && date) {
            loadStudentsAndAttendance(Number(selectedClass), date);
        }
    }

    function updateStatus(studentId: number, status: AttendanceStatus) {
        setAttendanceMap(prev => ({
            ...prev,
            [studentId]: status
        }));
    }

    /* ---------------- Save ---------------- */

    async function saveAttendance() {
        if (!selectedClass || !selectedDate || !selectedSchool) return;

        try {
            setLoading(prev => ({ ...prev, saving: true }));
            await api.post(`/api/attendance/bulk?date=${selectedDate}&schoolId=${selectedSchool}`, attendanceMap);
            alert("Attendance saved successfully!");
        } catch {
            alert("Failed to save attendance");
        } finally {
            setLoading(prev => ({ ...prev, saving: false }));
        }
    }

    /* ---------------- UI ---------------- */

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-gray-800">Student Attendance</h1>
                {selectedClass && students.length > 0 && (
                    <button
                        onClick={saveAttendance}
                        disabled={loading.saving}
                        className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition-all font-semibold disabled:bg-gray-400"
                    >
                        {loading.saving ? "Saving..." : "Save Attendance"}
                    </button>
                )}
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
                    <label className="block text-xs font-semibold uppercase text-gray-500 mb-2">Date</label>
                    <input
                        type="date"
                        value={selectedDate}
                        onChange={onDateChange}
                        className="w-full border-gray-200 rounded-lg px-4 py-2"
                    />
                </div>
            </div>

            {error && <div className="bg-red-50 text-red-600 p-4 rounded-lg border border-red-100">{error}</div>}

            {/* ---------------- Attendance Table ---------------- */}
            {selectedClass && !loading.students ? (
                <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-gray-50 text-gray-600 font-medium">
                            <tr>
                                <th className="p-4 text-left">Admission No</th>
                                <th className="p-4 text-left">Student Name</th>
                                <th className="p-4 text-center">Status</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {students.map((s) => (
                                <tr key={s.id} className="hover:bg-gray-50 transition-colors">
                                    <td className="p-4 font-medium text-gray-700">{s.admissionNumber}</td>
                                    <td className="p-4 text-gray-600">{s.firstName} {s.lastName}</td>
                                    <td className="p-4 text-center">
                                        <div className="flex justify-center gap-2">
                                            {(["PRESENT", "ABSENT", "LATE", "HALF_DAY"] as AttendanceStatus[]).map(status => (
                                                <button
                                                    key={status}
                                                    onClick={() => updateStatus(s.id, status)}
                                                    className={`
                            px-3 py-1 text-xs rounded-full font-semibold transition-all border
                            ${attendanceMap[s.id] === status
                                                            ? status === "PRESENT" ? "bg-green-100 text-green-700 border-green-200" :
                                                                status === "ABSENT" ? "bg-red-100 text-red-700 border-red-200" :
                                                                    status === "LATE" ? "bg-yellow-100 text-yellow-700 border-yellow-200" :
                                                                        "bg-orange-100 text-orange-700 border-orange-200"
                                                            : "bg-white text-gray-400 border-gray-100 hover:bg-gray-50"}
                          `}
                                                >
                                                    {status.replace('_', ' ')}
                                                </button>
                                            ))}
                                        </div>
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
                selectedClass && loading.students && (
                    <div className="text-center p-12 bg-gray-50 rounded-xl border border-gray-100 text-gray-400">
                        Loading students...
                    </div>
                )
            )}
        </div>
    );
}
