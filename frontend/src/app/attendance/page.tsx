"use client";

import { useEffect, useState } from "react";
import SessionSelect from "@/components/SessionSelect";
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

    /* ---------- Pagination ---------- */
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalStudents, setTotalStudents] = useState(0);
    const PAGE_SIZE = 50;

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
            setCurrentPage(0);

            const res = await api.get(`/api/classes/by-school/${schoolId}`);
            setClasses(res.data.content || []);
        } catch {
            setError("Failed to load classes");
        } finally {
            setLoading(prev => ({ ...prev, classes: false }));
        }
    }

    /* ---------------- Load Students & Attendance ---------------- */

    async function loadStudentsAndAttendance(classId: number, date: string, page: number) {
        try {
            setLoading(prev => ({ ...prev, students: true }));
            setError("");

            const cls = classes.find(c => c.id === classId);
            if (!cls) return;

            // 1. Load Students (Paginated)
            const studentRes = await studentApi.byClass(classId, page, PAGE_SIZE);
            const studentList = studentRes.data.content || [];
            setStudents(studentList);
            setTotalPages(studentRes.data.totalPages || 0);
            setTotalStudents(studentRes.data.totalElements || 0);

            // 2. Load Existing Attendance for the Class on this Date
            const attendanceRes = await api.get(`/api/attendance/class/${classId}?session=${cls.session}&date=${date}`);
            const existingAttendance: any[] = attendanceRes.data || [];

            const newMap = { ...attendanceMap };

            // Default students on current page to PRESENT if not already in map
            studentList.forEach((s: Student) => {
                if (!(s.id in newMap)) {
                    newMap[s.id] = "PRESENT";
                }
            });

            // Override with existing records from DB
            existingAttendance.forEach(record => {
                newMap[record.studentId] = record.status;
            });

            setAttendanceMap(newMap);

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
        setCurrentPage(0);
        if (classId && selectedDate) {
            loadStudentsAndAttendance(Number(classId), selectedDate, 0);
        }
    }

    function onDateChange(e: any) {
        const date = e.target.value;
        setSelectedDate(date);
        if (selectedClass && date) {
            loadStudentsAndAttendance(Number(selectedClass), date, currentPage);
        }
    }

    function changePage(newPage: number) {
        if (newPage < 0 || newPage >= totalPages) return;
        setCurrentPage(newPage);
        if (selectedClass && selectedDate) {
            loadStudentsAndAttendance(Number(selectedClass), selectedDate, newPage);
        }
    }

    function togglePresence(studentId: number) {
        setAttendanceMap(prev => ({
            ...prev,
            [studentId]: prev[studentId] === "PRESENT" ? "ABSENT" : "PRESENT"
        }));
    }

    function updateStatus(studentId: number, status: AttendanceStatus) {
        setAttendanceMap(prev => ({
            ...prev,
            [studentId]: status
        }));
    }

    function markAllPresent() {
        const newMap = { ...attendanceMap };
        students.forEach(s => {
            newMap[s.id] = "PRESENT";
        });
        setAttendanceMap(newMap);
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
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">Student Attendance</h1>
                    {selectedClass && <p className="text-gray-500 text-sm">Total Students: {totalStudents}</p>}
                </div>

                <div className="flex gap-3">
                    {selectedClass && students.length > 0 && (
                        <>
                            <button
                                onClick={markAllPresent}
                                className="bg-green-50 text-green-700 px-4 py-2 rounded-lg hover:bg-green-100 transition-all font-medium border border-green-200"
                            >
                                Mark Current Page as Present
                            </button>
                            <button
                                onClick={saveAttendance}
                                disabled={loading.saving}
                                className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition-all font-semibold shadow-sm disabled:bg-gray-400"
                            >
                                {loading.saving ? "Saving..." : "Save All Marked"}
                            </button>
                        </>
                    )}
                </div>
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
                        className="w-full border-gray-200 rounded-lg px-4 py-2 disabled:bg-gray-100 disabled:text-gray-400"
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
                <div className="space-y-4">
                    <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
                        <table className="w-full text-sm">
                            <thead className="bg-gray-50 text-gray-600 font-medium border-b border-gray-100">
                                <tr>
                                    <th className="p-4 text-center w-20">Present</th>
                                    <th className="p-4 text-left w-1/4">Admission No</th>
                                    <th className="p-4 text-left w-1/3">Student Name</th>
                                    <th className="p-4 text-center">Other Status</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {students.map((s) => (
                                    <tr key={s.id} className="hover:bg-gray-50 transition-colors">
                                        <td className="p-4 text-center">
                                            <input
                                                type="checkbox"
                                                checked={attendanceMap[s.id] === "PRESENT"}
                                                onChange={() => togglePresence(s.id)}
                                                className="w-5 h-5 text-blue-600 border-gray-300 rounded focus:ring-blue-500 cursor-pointer"
                                            />
                                        </td>
                                        <td className="p-4 font-medium text-gray-700">{s.admissionNumber}</td>
                                        <td className="p-4 text-gray-600">{s.firstName} {s.lastName}</td>
                                        <td className="p-4 text-center">
                                            <div className="flex justify-center gap-2">
                                                {(["LATE", "HALF_DAY"] as AttendanceStatus[]).map(status => (
                                                    <button
                                                        key={status}
                                                        onClick={() => updateStatus(s.id, status)}
                                                        className={`
                                                px-3 py-1 text-xs rounded-full font-semibold transition-all border
                                                ${attendanceMap[s.id] === status
                                                                ? status === "LATE" ? "bg-yellow-100 text-yellow-700 border-yellow-200" :
                                                                    "bg-orange-100 text-orange-700 border-orange-200"
                                                                : "bg-white text-gray-400 border-gray-100 hover:bg-gray-50"}
                                            `}
                                                    >
                                                        {status.replace('_', ' ')}
                                                    </button>
                                                ))}
                                                {attendanceMap[s.id] === "ABSENT" && (
                                                    <span className="bg-red-100 text-red-700 border border-red-200 px-3 py-1 text-xs rounded-full font-semibold">
                                                        ABSENT
                                                    </span>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                                {students.length === 0 && (
                                    <tr>
                                        <td colSpan={4} className="p-12 text-center text-gray-400 italic">
                                            No students found in this class
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>

                    {/* ---------- Pagination Controls ---------- */}
                    {totalPages > 1 && (
                        <div className="flex justify-between items-center bg-white p-4 border rounded-xl shadow-sm">
                            <span className="text-sm text-gray-500">
                                Page {currentPage + 1} of {totalPages}
                            </span>
                            <div className="flex gap-2">
                                <button
                                    disabled={currentPage === 0}
                                    onClick={() => changePage(currentPage - 1)}
                                    className="px-4 py-2 border rounded hover:bg-gray-50 disabled:bg-gray-50 disabled:text-gray-300 transition-colors"
                                >
                                    Previous
                                </button>
                                <button
                                    disabled={currentPage === totalPages - 1}
                                    onClick={() => changePage(currentPage + 1)}
                                    className="px-4 py-2 border rounded hover:bg-gray-50 disabled:bg-gray-50 disabled:text-gray-300 transition-colors"
                                >
                                    Next
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            ) : (
                selectedClass && loading.students && (
                    <div className="text-center p-12 bg-gray-50 rounded-xl border border-gray-100 text-gray-400 italic">
                        Fetching student roster...
                    </div>
                )
            )}
        </div>
    );
}
