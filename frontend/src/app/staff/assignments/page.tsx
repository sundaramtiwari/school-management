"use client";

import { useCallback, useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { useSession } from "@/context/SessionContext";
import { useToast } from "@/components/ui/Toast";

type Teacher = { id: number; fullName: string };
type SchoolClass = { id: number; name: string; section: string; sessionId: number };
type Subject = { id: number; name: string };
type Assignment = {
  id: number;
  teacherName: string;
  className: string;
  subjectName: string;
  sessionName: string;
  status?: string;
};

const ALLOWED_ROLES = ["SCHOOL_ADMIN", "SUPER_ADMIN", "PLATFORM_ADMIN"];

export default function TeacherAssignmentsPage() {
  const { user } = useAuth();
  const { currentSession } = useSession();
  const { showToast } = useToast();

  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [assignments, setAssignments] = useState<Assignment[]>([]);

  const [form, setForm] = useState({
    teacherId: "" as number | "",
    classId: "" as number | "",
    subjectId: "" as number | "",
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadingAssignments, setLoadingAssignments] = useState(false);
  const [loadingSubjects, setLoadingSubjects] = useState(false);

  const role = user?.role?.toUpperCase();
  const allowed = role && ALLOWED_ROLES.includes(role);

  // 1. Load Teachers on mount
  const loadTeachers = useCallback(async () => {
    try {
      const res = await api.get<Teacher[]>("/api/teachers");
      setTeachers(res.data || []);
    } catch {
      showToast("Failed to load teachers", "error");
    }
  }, [showToast]);

  // 2. Load Classes & Assignments for the active session
  const loadSessionData = useCallback(async () => {
    if (!currentSession) return;
    try {
      setLoadingAssignments(true);
      const [classesRes, assignmentsRes] = await Promise.all([
        api.get<{ content: SchoolClass[] }>(`/api/classes/mine/session/${currentSession.id}?size=100`),
        api.get<Assignment[]>(`/api/class-subjects/assignments?sessionId=${currentSession.id}`),
      ]);
      setClasses(classesRes.data?.content || []);
      setAssignments((assignmentsRes.data || []).map(a => ({ ...a, status: "ACTIVE" })));
    } catch {
      showToast("Failed to load session data", "error");
      setClasses([]);
      setAssignments([]);
    } finally {
      setLoadingAssignments(false);
    }
  }, [currentSession, showToast]);

  // 3. Load Subjects when Class changes (via /api/class-subjects/by-class/{classId})
  const loadSubjectsForClass = useCallback(async (classId: number) => {
    try {
      setLoadingSubjects(true);
      type ClassSubjectDto = { subjectId: number; subjectName: string };
      const res = await api.get<{ content: ClassSubjectDto[] }>(`/api/class-subjects/by-class/${classId}?size=100`);
      setSubjects((res.data?.content || []).map((cs) => ({ id: cs.subjectId, name: cs.subjectName })));
    } catch {
      showToast("Failed to load subjects for class", "error");
      setSubjects([]);
    } finally {
      setLoadingSubjects(false);
    }
  }, [showToast]);

  // Effects
  useEffect(() => {
    if (allowed) loadTeachers();
  }, [allowed, loadTeachers]);

  useEffect(() => {
    if (allowed && currentSession) loadSessionData();
  }, [allowed, currentSession, loadSessionData]);

  useEffect(() => {
    if (form.classId) {
      loadSubjectsForClass(form.classId as number);
    } else {
      setSubjects([]);
    }
  }, [form.classId, loadSubjectsForClass]);

  if (!allowed) {
    return <div className="text-gray-500">You do not have permission to access this page.</div>;
  }

  function updateForm(e: ChangeEvent<HTMLSelectElement>) {
    const { name, value } = e.target;
    const num = value ? Number(value) : "";
    setForm((prev) => {
      const next = { ...prev, [name]: num };
      if (name === "classId") next.subjectId = "";
      return next;
    });
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!currentSession) {
      showToast("No active session", "error");
      return;
    }
    if (!form.teacherId || !form.classId || !form.subjectId) {
      showToast("All fields are required", "error");
      return;
    }
    try {
      setIsSubmitting(true);
      await api.post("/api/class-subjects/assignments", {
        teacherId: form.teacherId,
        sessionId: currentSession.id,
        classId: form.classId,
        subjectId: form.subjectId,
      });
      showToast("Assignment created successfully", "success");
      setForm({ teacherId: "", classId: "", subjectId: "" });
      loadSessionData();
    } catch (err: unknown) {
      const msg = err && typeof err === "object" && "response" in err
        ? String((err as { response?: { data?: { message?: string } } }).response?.data?.message || "Request failed")
        : "Request failed";
      showToast(msg, "error");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDeactivate(id: number) {
    try {
      await api.delete(`/api/class-subjects/assignments/${id}`);
      showToast("Assignment deactivated", "success");
      loadSessionData();
    } catch {
      showToast("Failed to deactivate assignment", "error");
    }
  }

  const inputCls =
    "w-full rounded-md border border-gray-300 px-3 py-2 text-base focus:ring-2 focus:ring-blue-500 focus:outline-none disabled:bg-gray-100 disabled:text-gray-400";

  return (
    <div className="mx-auto px-6 py-6 space-y-6">
      <h1 className="text-lg font-semibold text-gray-800">Teacher Assignments</h1>

      {/* Assignment Form */}
      <div className="bg-white rounded-lg shadow border border-gray-100 p-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">

            {/* Session (Read-Only) */}
            <div>
              <label className="block text-xs font-bold text-gray-500 uppercase mb-2">Session</label>
              <div className={`px-3 py-2 rounded-md border border-gray-200 bg-gray-50 text-gray-700 font-medium ${!currentSession ? "text-red-500" : ""}`}>
                {currentSession ? `${currentSession.name} (Active)` : "No Active Session"}
              </div>
            </div>

            {/* Teacher */}
            <div>
              <label className="block text-xs font-bold text-gray-500 uppercase mb-2">Teacher</label>
              <select name="teacherId" value={form.teacherId} onChange={updateForm} className={inputCls} required disabled={!currentSession}>
                <option value="">Select Teacher</option>
                {teachers.map((t) => (
                  <option key={t.id} value={t.id}>{t.fullName}</option>
                ))}
              </select>
            </div>

            {/* Class */}
            <div>
              <label className="block text-xs font-bold text-gray-500 uppercase mb-2">Class</label>
              <select name="classId" value={form.classId} onChange={updateForm} className={inputCls} required disabled={!currentSession}>
                <option value="">Select Class</option>
                {classes.map((c) => (
                  <option key={c.id} value={c.id}>{c.name} {c.section || ""}</option>
                ))}
              </select>
            </div>

            {/* Subject (Dependent on Class) */}
            <div>
              <label className="block text-xs font-bold text-gray-500 uppercase mb-2">Subject</label>
              <select name="subjectId" value={form.subjectId} onChange={updateForm} className={inputCls} required disabled={!form.classId || loadingSubjects || !currentSession}>
                <option value="">{loadingSubjects ? "Loading..." : "Select Subject"}</option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            </div>
          </div>

          <button type="submit" disabled={isSubmitting || !currentSession} className="bg-blue-600 text-white px-6 py-2.5 rounded-md font-medium hover:bg-blue-700 disabled:opacity-50">
            {isSubmitting ? "Saving..." : "Assign"}
          </button>
        </form>
      </div>

      {/* Active Assignments Table */}
      <div className="bg-white rounded-lg shadow border border-gray-100 p-6">
        <h2 className="text-base font-semibold text-gray-700 mb-4">Active Assignments</h2>

        {!currentSession ? (
          <p className="text-gray-500 italic">Please set an active session to view assignments.</p>
        ) : loadingAssignments ? (
          <p className="text-gray-500">Loading...</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-gray-600 font-semibold border-b border-gray-100">
                <tr>
                  <th className="px-4 py-3 text-left">Teacher</th>
                  <th className="px-4 py-3 text-left">Class</th>
                  <th className="px-4 py-3 text-left">Subject</th>
                  <th className="px-4 py-3 text-left">Session</th>
                  <th className="px-4 py-3 text-left">Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {assignments.map((a) => (
                  <tr key={a.id} className="hover:bg-gray-50/50">
                    <td className="px-4 py-3 text-gray-800">{a.teacherName}</td>
                    <td className="px-4 py-3 text-gray-700">{a.className}</td>
                    <td className="px-4 py-3 text-gray-700">{a.subjectName}</td>
                    <td className="px-4 py-3 text-gray-700">{a.sessionName}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${a.status === "ACTIVE" ? "bg-green-100 text-green-800" : "bg-gray-100 text-gray-800"}`}>
                        {a.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      {a.status === "ACTIVE" && (
                        <button type="button" onClick={() => handleDeactivate(a.id)} className="bg-red-50 text-red-600 px-3 py-1.5 rounded-md text-xs font-medium hover:bg-red-100 border border-red-200">
                          Deactivate
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
                {assignments.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-gray-500">
                      No active assignments found for this session.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
