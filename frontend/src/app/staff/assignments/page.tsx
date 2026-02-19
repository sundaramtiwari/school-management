"use client";

import { useCallback, useEffect, useState, type ChangeEvent } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/components/ui/Toast";

type Session = { id: number; name: string };
type Teacher = { id: number; fullName: string };
type SchoolClass = { id: number; name: string; section: string; sessionId: number };
type Subject = { id: number; name: string };
type Assignment = {
  id: number;
  teacherName: string;
  className: string;
  subjectName: string;
  sessionName: string;
  status: string;
};

const ALLOWED_ROLES = ["SCHOOL_ADMIN", "SUPER_ADMIN", "PLATFORM_ADMIN"];

export default function TeacherAssignmentsPage() {
  const { user } = useAuth();
  const { showToast } = useToast();

  const [sessions, setSessions] = useState<Session[]>([]);
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [assignments, setAssignments] = useState<Assignment[]>([]);

  const [form, setForm] = useState({
    sessionId: "" as number | "",
    teacherId: "" as number | "",
    classId: "" as number | "",
    subjectId: "" as number | "",
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadingAssignments, setLoadingAssignments] = useState(false);

  const role = user?.role?.toUpperCase();
  const allowed = role && ALLOWED_ROLES.includes(role);

  const loadInitial = useCallback(async () => {
    try {
      const [sessionsRes, teachersRes, subjectsRes] = await Promise.all([
        api.get<Session[]>("/api/academic-sessions"),
        api.get<Teacher[]>("/api/teachers"),
        api.get<{ content: Subject[] }>("/api/subjects/mine?size=100"),
      ]);
      setSessions(sessionsRes.data || []);
      setTeachers(teachersRes.data || []);
      setSubjects(subjectsRes.data?.content || []);
    } catch {
      showToast("Failed to load data", "error");
    }
  }, [showToast]);

  const loadClassesAndAssignments = useCallback(
    async (sessionId: number) => {
      try {
        setLoadingAssignments(true);
        const [classesRes, assignmentsRes] = await Promise.all([
          api.get<{ content: SchoolClass[] }>(`/api/classes/mine/session/${sessionId}?size=100`),
          api.get<Assignment[]>(`/api/teacher-assignments?sessionId=${sessionId}`),
        ]);
        setClasses(classesRes.data?.content || []);
        setAssignments(assignmentsRes.data || []);
      } catch {
        showToast("Failed to load classes or assignments", "error");
        setClasses([]);
        setAssignments([]);
      } finally {
        setLoadingAssignments(false);
      }
    },
    [showToast]
  );

  useEffect(() => {
    if (allowed) loadInitial();
  }, [allowed, loadInitial]);

  useEffect(() => {
    if (form.sessionId) {
      loadClassesAndAssignments(form.sessionId);
    } else {
      setClasses([]);
      setAssignments([]);
    }
  }, [form.sessionId, loadClassesAndAssignments]);

  if (!allowed) {
    return (
      <div className="text-gray-500">You do not have permission to access this page.</div>
    );
  }

  function updateForm(e: ChangeEvent<HTMLSelectElement>) {
    const { name, value } = e.target;
    const num = value ? Number(value) : "";
    setForm((prev) => {
      const next = { ...prev, [name]: num };
      if (name === "sessionId") {
        next.teacherId = "";
        next.classId = "";
        next.subjectId = "";
      } else if (name === "classId") {
        next.subjectId = "";
      }
      return next;
    });
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (
      !form.sessionId ||
      !form.teacherId ||
      !form.classId ||
      !form.subjectId
    ) {
      showToast("All fields are required", "error");
      return;
    }
    try {
      setIsSubmitting(true);
      await api.post("/api/teacher-assignments", {
        teacherId: form.teacherId,
        sessionId: form.sessionId,
        classId: form.classId,
        subjectId: form.subjectId,
      });
      showToast("Assignment created successfully", "success");
      setForm((prev) => ({
        ...prev,
        teacherId: "",
        classId: "",
        subjectId: "",
      }));
      if (form.sessionId) {
        loadClassesAndAssignments(form.sessionId);
      }
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? String((err as { response?: { data?: { message?: string } } }).response?.data?.message || "Request failed")
          : "Request failed";
      showToast(msg, "error");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDeactivate(id: number) {
    try {
      await api.delete(`/api/teacher-assignments/${id}`);
      showToast("Assignment deactivated", "success");
      if (form.sessionId) {
        loadClassesAndAssignments(form.sessionId);
      }
    } catch {
      showToast("Failed to deactivate assignment", "error");
    }
  }

  const inputCls =
    "w-full rounded-md border border-gray-300 px-3 py-2 text-base focus:ring-2 focus:ring-blue-500 focus:outline-none";

  return (
    <div className="mx-auto px-6 py-6 space-y-6">
      <h1 className="text-lg font-semibold text-gray-800">Teacher Assignments</h1>

      {/* Section 1 — Assignment Form */}
      <div className="bg-white rounded-lg shadow border border-gray-100 p-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div>
              <label className="block text-xs font-bold text-gray-500 uppercase mb-2">
                Session
              </label>
              <select
                name="sessionId"
                value={form.sessionId}
                onChange={updateForm}
                className={inputCls}
                required
              >
                <option value="">Select Session</option>
                {sessions.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-500 uppercase mb-2">
                Teacher
              </label>
              <select
                name="teacherId"
                value={form.teacherId}
                onChange={updateForm}
                className={inputCls}
                required
              >
                <option value="">Select Teacher</option>
                {teachers.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.fullName}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-500 uppercase mb-2">
                Class
              </label>
              <select
                name="classId"
                value={form.classId}
                onChange={updateForm}
                className={inputCls}
                required
                disabled={!form.sessionId}
              >
                <option value="">Select Class</option>
                {classes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} {c.section || ""}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-500 uppercase mb-2">
                Subject
              </label>
              <select
                name="subjectId"
                value={form.subjectId}
                onChange={updateForm}
                className={inputCls}
                required
              >
                <option value="">Select Subject</option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <button
            type="submit"
            disabled={isSubmitting}
            className="bg-blue-600 text-white px-6 py-2.5 rounded-md font-medium hover:bg-blue-700 disabled:opacity-50"
          >
            {isSubmitting ? "Saving..." : "Assign"}
          </button>
        </form>
      </div>

      {/* Section 2 — Active Assignments Table */}
      <div className="bg-white rounded-lg shadow border border-gray-100 p-6">
        <h2 className="text-base font-semibold text-gray-700 mb-4">Active Assignments</h2>
        {loadingAssignments ? (
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
                      <span className="text-gray-600">{a.status}</span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        type="button"
                        onClick={() => handleDeactivate(a.id)}
                        className="bg-red-600 text-white px-3 py-1.5 rounded-md text-sm font-medium hover:bg-red-700"
                      >
                        Deactivate
                      </button>
                    </td>
                  </tr>
                ))}
                {assignments.length === 0 && (
                  <tr>
                    <td
                      colSpan={6}
                      className="px-4 py-8 text-center text-gray-500"
                    >
                      {form.sessionId
                        ? "No active assignments for this session."
                        : "Select a session above to view or create assignments."}
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
