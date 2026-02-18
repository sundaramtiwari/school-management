"use client";

import { ChangeEvent, useCallback, useEffect, useState } from "react";
import { studentApi } from "@/lib/studentApi";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { useSession } from "@/context/SessionContext";
import { useAuth } from "@/context/AuthContext";

/* ---------------- Types ---------------- */

type SchoolClass = {
    id: number;
    name: string;
    section: string;
    sessionId: number;
};

type Student = {
    id: number;
    firstName: string;
    lastName: string;
    admissionNumber: string;
};

type AttendanceStatus = "PRESENT" | "ABSENT" | "LATE" | "HALF_DAY";
type AttendanceRecord = { studentId: number; status: AttendanceStatus };
type AttendanceClassResponse = {
    attendanceList: AttendanceRecord[];
    editable: boolean;
    committed: boolean;
};

/* ---------------- Page ---------------- */

export default function AttendancePage() {
    const { user } = useAuth();
    const { showToast } = useToast();
    const { currentSession } = useSession();

    const isPlatformAdmin = user?.role === "SUPER_ADMIN" || user?.role === "PLATFORM_ADMIN";
    const canCommit = user?.role === "SCHOOL_ADMIN" || user?.role === "TEACHER" || isPlatformAdmin;
    const canModify = user?.role === "SCHOOL_ADMIN" || isPlatformAdmin;

    /* ---------- Filters ---------- */

    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [selectedClass, setSelectedClass] = useState<number | "">("");
    const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().split('T')[0]);

    /* ---------- Data ---------- */

    const [students, setStudents] = useState<Student[]>([]);
    const [attendanceMap, setAttendanceMap] = useState<Record<number, AttendanceStatus>>({});
    const [editable, setEditable] = useState(true);
    const [committed, setCommitted] = useState(false);
    const [isModifying, setIsModifying] = useState(false);

    /* ---------- Pagination ---------- */
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const PAGE_SIZE = 50;

    const [loading, setLoading] = useState({
        classes: true,
        students: false,
        saving: false,
    });

    const getErrorMessage = (err: unknown): string => {
        if (typeof err === "object" && err !== null && "response" in err) {
            const response = (err as { response?: { data?: { message?: string } } }).response;
            if (response?.data?.message) return response.data.message;
        }
        if (typeof err === "object" && err !== null && "message" in err) {
            const message = (err as { message?: string }).message;
            if (message) return message;
        }
        return "Unknown error";
    };

    /* ---------------- Init ---------------- */

    const loadClasses = useCallback(async () => {
        try {
            setLoading(prev => ({ ...prev, classes: true }));
            const res = await api.get("/api/classes/mine");
            setClasses(res.data.content || []);
        } catch {
            showToast("Failed to load classes", "error");
        } finally {
            setLoading(prev => ({ ...prev, classes: false }));
        }
    }, [showToast]);

    useEffect(() => {
        void loadClasses();
    }, [currentSession, loadClasses]);

    /* ---------------- Load Students & Attendance ---------------- */

    async function loadStudentsAndAttendance(classId: number, date: string, page: number) {
        try {
            setLoading(prev => ({ ...prev, students: true }));

            if (!currentSession) return;

            // 1. Load Students (Paginated)
            const studentRes = await studentApi.byClass(classId, page, PAGE_SIZE);
            const studentList = studentRes.data.content || [];
            setStudents(studentList);
            setTotalPages(studentRes.data.totalPages || 0);

            // 2. Load Existing Attendance for the Class on this Date
            const attendanceRes = await api.get<AttendanceClassResponse>(`/api/attendance/class/${classId}?sessionId=${currentSession.id}&date=${date}`);
            const { attendanceList, editable: isEditable, committed: isCommitted } = attendanceRes.data;
            setEditable(isEditable);
            setCommitted(isCommitted);
            setIsModifying(false); // Reset modifying state on new load

            const newMap = { ...attendanceMap };

            // Default students on current page to PRESENT if not already in map
            studentList.forEach((s: Student) => {
                if (!(s.id in newMap)) {
                    newMap[s.id] = "PRESENT";
                }
            });

            // Override with existing records from DB
            attendanceList.forEach((record) => {
                newMap[record.studentId] = record.status;
            });

            setAttendanceMap(newMap);

        } catch (e: unknown) {
            const msg = getErrorMessage(e);
            showToast("Failed to load attendance: " + msg, "error");
        } finally {
            setLoading(prev => ({ ...prev, students: false }));
        }
    }

    /* ---------------- Handlers ---------------- */

    function onClassChange(e: ChangeEvent<HTMLSelectElement>) {
        const classId = e.target.value;
        setSelectedClass(classId);
        setCurrentPage(0);
        if (classId && selectedDate) {
            void loadStudentsAndAttendance(Number(classId), selectedDate, 0);
        }
    }

    function onDateChange(e: ChangeEvent<HTMLInputElement>) {
        const date = e.target.value;
        setSelectedDate(date);
        if (selectedClass && date) {
            void loadStudentsAndAttendance(Number(selectedClass), date, currentPage);
        }
    }

    function changePage(newPage: number) {
        if (newPage < 0 || newPage >= totalPages) return;
        setCurrentPage(newPage);
        if (selectedClass && selectedDate) {
            void loadStudentsAndAttendance(Number(selectedClass), selectedDate, newPage);
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
        showToast("Marked current page as PRESENT", "info");
    }

    /* ---------------- Save ---------------- */

    async function saveAttendance() {
        if (!selectedClass || !selectedDate || !currentSession) return;

        try {
            setLoading(prev => ({ ...prev, saving: true }));
            await api.post(`/api/attendance/bulk?date=${selectedDate}&classId=${selectedClass}&sessionId=${currentSession.id}`, attendanceMap);
            showToast("Attendance records saved successfully!", "success");
        } catch (e: unknown) {
            const msg = getErrorMessage(e);
            showToast("Save failed: " + msg, "error");
        } finally {
            setLoading(prev => ({ ...prev, saving: false }));
        }
    }

    /* ---------------- UI ---------------- */

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center text-wrap">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Student Attendance</h1>
                    <p className="text-gray-500">Track and record daily student presence for <span className="text-blue-600 font-bold">{currentSession?.name || "current session"}</span>.</p>
                </div>

                <div className="flex gap-3">
                    {canCommit && selectedClass && students.length > 0 && (editable && (!committed || isModifying)) && (
                        <>
                            <button
                                onClick={markAllPresent}
                                className="bg-green-50 text-green-700 px-5 py-2.5 rounded-xl hover:bg-green-100 transition-all font-bold border border-green-200 shadow-sm"
                            >
                                Mark Page Present
                            </button>
                            <button
                                onClick={saveAttendance}
                                disabled={loading.saving}
                                className="bg-blue-600 text-white px-8 py-2.5 rounded-xl hover:bg-blue-700 transition-all font-bold shadow-lg disabled:bg-gray-400"
                            >
                                {loading.saving ? "Saving..." : "Commit Attendance"}
                            </button>
                        </>
                    )}
                </div>
            </div>

            {/* ---------------- Filters ---------------- */}
            <div className="bg-white p-6 border rounded-2xl shadow-sm flex flex-wrap gap-6 items-end">
                <div className="flex-1 min-w-[250px]">
                    <label className="block text-xs font-bold uppercase text-gray-400 mb-2 ml-1">Class Selection</label>
                    <select
                        value={selectedClass}
                        onChange={onClassChange}
                        disabled={loading.classes}
                        className="input-ref"
                    >
                        <option value="">Select Academic Class</option>
                        {classes.map(c => (
                            <option key={c.id} value={c.id}>
                                {c.name} {c.section}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="flex-1 min-w-[250px]">
                    <label className="block text-xs font-bold uppercase text-gray-400 mb-2 ml-1">Attendance Date</label>
                    <input
                        type="date"
                        value={selectedDate}
                        onChange={onDateChange}
                        className="input-ref"
                    />
                </div>
            </div>

            {/* ---------------- Attendance Table ---------------- */}
            {!editable && selectedClass && (
                <div className="bg-yellow-50 border border-yellow-200 text-yellow-800 px-6 py-4 rounded-xl flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                        <span className="text-2xl">⚠️</span>
                        <div>
                            <p className="font-semibold">Attendance Locked</p>
                            <p className="text-sm">This date is locked. Only admins can modify past attendance records.</p>
                        </div>
                    </div>
                </div>
            )}

            {editable && committed && !isModifying && (
                <div className="bg-blue-50 border border-blue-200 text-blue-800 px-6 py-4 rounded-xl flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                        <span className="text-2xl">ℹ️</span>
                        <div>
                            <p className="font-semibold">Attendance Committed</p>
                            <p className="text-sm">Attendance has already been marked for this date.</p>
                        </div>
                    </div>
                    {canModify && (
                        <button
                            onClick={() => setIsModifying(true)}
                            className="bg-blue-600 text-white px-4 py-2 rounded-lg font-semibold hover:bg-blue-700 transition shadow-sm"
                        >
                            Modify Attendance
                        </button>
                    )}
                </div>
            )}

            {selectedClass && loading.students ? (
                <div className="bg-white p-8 rounded-2xl border">
                    <TableSkeleton rows={12} cols={4} />
                </div>
            ) : selectedClass ? (
                <fieldset disabled={!editable || (committed && !isModifying)} className="space-y-4">
                    <div className="bg-white border rounded-2xl shadow-sm overflow-hidden">
                        <table className="w-full text-sm">
                            <thead className="bg-gray-50 text-gray-600 font-bold border-b">
                                <tr>
                                    <th className="p-4 text-center w-24">Presence</th>
                                    <th className="p-4 text-left">Student Info</th>
                                    <th className="p-4 text-center">Admission No</th>
                                    <th className="p-4 text-center">Status Flag</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {students.map((s) => (
                                    <tr key={s.id} className="hover:bg-gray-50/50 transition-colors">
                                        <td className="p-4 text-center">
                                            <div className="flex justify-center">
                                                <input
                                                    type="checkbox"
                                                    checked={attendanceMap[s.id] === "PRESENT"}
                                                    onChange={() => togglePresence(s.id)}
                                                    className="w-6 h-6 text-blue-600 border-gray-300 rounded-lg focus:ring-blue-500 cursor-pointer shadow-sm transition-all"
                                                />
                                            </div>
                                        </td>
                                        <td className="p-4">
                                            <span className="font-bold text-gray-800">{s.firstName} {s.lastName}</span>
                                        </td>
                                        <td className="p-4 text-center font-mono text-gray-600">{s.admissionNumber}</td>
                                        <td className="p-4 text-center">
                                            <div className="flex justify-center gap-2">
                                                {(["LATE", "HALF_DAY"] as AttendanceStatus[]).map(status => (
                                                    <button
                                                        key={status}
                                                        onClick={() => updateStatus(s.id, status)}
                                                        className={`
                                                px-3 py-1.5 text-[10px] rounded-full font-bold transition-all border
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
                                                    <span className="bg-red-50 text-red-600 border border-red-100 px-3 py-1.5 text-[10px] rounded-full font-bold uppercase tracking-wider">
                                                        ABSENT
                                                    </span>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                                {students.length === 0 && (
                                    <tr>
                                        <td colSpan={4} className="p-20 text-center text-gray-400 italic">
                                            No students found in this class record.
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>

                    {/* ---------- Pagination Controls ---------- */}
                    {totalPages > 1 && (
                        <div className="flex justify-between items-center bg-white px-6 py-4 border rounded-2xl shadow-sm">
                            <span className="text-sm font-medium text-gray-500">
                                Page <span className="text-blue-600 font-bold">{currentPage + 1}</span> of <span className="font-bold">{totalPages}</span>
                            </span>
                            <div className="flex gap-2">
                                <button
                                    disabled={currentPage === 0}
                                    onClick={() => changePage(currentPage - 1)}
                                    className="px-4 py-2 border rounded-xl hover:bg-gray-50 disabled:opacity-50 transition-all font-semibold"
                                >
                                    Previous
                                </button>
                                <button
                                    disabled={currentPage === totalPages - 1}
                                    onClick={() => changePage(currentPage + 1)}
                                    className="px-4 py-2 border rounded-xl hover:bg-gray-50 disabled:opacity-50 transition-all font-semibold"
                                >
                                    Next
                                </button>
                            </div>
                        </div>
                    )}
                </fieldset>
            ) : (
                <div className="p-20 text-center bg-gray-50 rounded-2xl border border-dashed border-gray-300 text-gray-400 italic">
                    Select a class and date to manage attendance records.
                </div>
            )}
        </div>
    );
}
