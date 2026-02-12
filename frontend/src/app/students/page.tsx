"use client";

import { useEffect, useState } from "react";
import { studentApi } from "@/lib/studentApi";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";
import { useSession } from "@/context/SessionContext";
import Modal from "@/components/ui/Modal";
import { TableSkeleton } from "@/components/ui/Skeleton";

/* ---------------- Types ---------------- */

type SchoolClass = {
  id: number;
  name: string;
  section: string;
};

type Student = {
  id: number;
  firstName: string;
  lastName: string;
  admissionNumber: string;
  gender: string;
  contactNumber: string;
};

/* ---------------- Page ---------------- */

export default function StudentsPage() {
  const { showToast } = useToast();
  const { currentSession } = useSession();

  /* ---------- Filters ---------- */

  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [selectedClass, setSelectedClass] = useState<number | "">("");

  /* ---------- Students ---------- */

  const [students, setStudents] = useState<Student[]>([]);
  const [loadingStudents, setLoadingStudents] = useState(false);

  /* ---------- Loading ---------- */

  const [loadingClasses, setLoadingClasses] = useState(true);
  const [error, setError] = useState("");

  /* ---------- Form State ---------- */

  const [showAddModal, setShowAddModal] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [studentForm, setStudentForm] = useState({
    admissionNumber: "",
    firstName: "",
    lastName: "",
    gender: "",
    contactNumber: "",
    email: "",
    classId: "",
  });

  /* ---------------- Init ---------------- */

  useEffect(() => {
    loadClasses();
  }, [currentSession]);

  async function loadClasses() {
    try {
      setLoadingClasses(true);
      const res = await api.get("/api/classes/mine");
      setClasses(res.data.content || []);
    } catch {
      setError("Failed to load classes");
      showToast("Error loading classes", "error");
    } finally {
      setLoadingClasses(false);
    }
  }

  async function loadStudents(classId: number) {
    try {
      setLoadingStudents(true);
      const res = await studentApi.byClass(classId, 0, 50);
      setStudents(res.data.content || []);
    } catch {
      showToast("Failed to load students", "error");
    } finally {
      setLoadingStudents(false);
    }
  }

  /* ---------------- Handlers ---------------- */

  function onClassChange(e: any) {
    const value = e.target.value;
    setSelectedClass(value);
    setStudents([]);
    if (value) {
      loadStudents(Number(value));
    }
  }

  function updateStudentField(e: any) {
    setStudentForm({
      ...studentForm,
      [e.target.name]: e.target.value,
    });
  }

  async function saveStudent() {
    if (!studentForm.firstName || !studentForm.gender || !studentForm.classId || !studentForm.admissionNumber || !currentSession) {
      showToast(currentSession ? "Please fill all required fields" : "No active session", "warning");
      return;
    }

    try {
      setIsSaving(true);
      const res = await studentApi.create({
        admissionNumber: studentForm.admissionNumber,
        firstName: studentForm.firstName,
        lastName: studentForm.lastName,
        gender: studentForm.gender,
        contactNumber: studentForm.contactNumber,
        email: studentForm.email,
      });

      const studentId = res.data.id;
      await studentApi.enroll({
        studentId,
        classId: studentForm.classId,
        sessionId: currentSession.id,
      });

      showToast("Student enrolled successfully!", "success");
      setShowAddModal(false);

      setStudentForm({
        admissionNumber: "",
        firstName: "",
        lastName: "",
        gender: "",
        contactNumber: "",
        email: "",
        classId: "",
      });

      if (Number(studentForm.classId) === Number(selectedClass)) {
        loadStudents(Number(selectedClass));
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data || err.message;
      showToast("Failed to save student: " + msg, "error");
    } finally {
      setIsSaving(false);
    }
  }

  /* ---------------- UI ---------------- */

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">Student Directory</h1>
          <p className="text-gray-500">Manage students for <span className="text-blue-600 font-bold">{currentSession?.name || "current session"}</span>.</p>
        </div>

        <button
          onClick={() => {
            if (selectedClass) {
              setStudentForm(prev => ({ ...prev, classId: selectedClass.toString() }));
            }
            setShowAddModal(true);
          }}
          className="bg-blue-600 text-white px-6 py-2.5 rounded-xl font-bold shadow-lg hover:bg-blue-700 transition-all flex items-center gap-2"
        >
          <span className="text-xl">+</span> Add Student
        </button>
      </div>

      <div className="bg-white p-6 border rounded-2xl shadow-sm flex flex-wrap gap-4 items-end">
        <div className="flex-1 min-w-[300px]">
          <label className="block text-xs font-bold uppercase text-gray-400 mb-2 ml-1">Filter by Class</label>
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

      {loadingStudents ? (
        <div className="bg-white p-8 rounded-2xl border">
          <TableSkeleton rows={10} cols={4} />
        </div>
      ) : selectedClass ? (
        <div className="bg-white border rounded-2xl shadow-sm overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600 font-bold border-b">
              <tr>
                <th className="p-4 text-left">Student Name</th>
                <th className="p-4 text-center">Admission No</th>
                <th className="p-4 text-center">Gender</th>
                <th className="p-4 text-center">Contact</th>
                <th className="p-4 text-center w-24">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {students.map((s) => (
                <tr key={s.id} className="hover:bg-gray-50/50 transition-colors">
                  <td className="p-4 text-gray-800 font-bold">
                    {s.firstName} {s.lastName}
                  </td>
                  <td className="p-4 text-center font-mono text-gray-600">{s.admissionNumber || "-"}</td>
                  <td className="p-4 text-center">
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase border ${s.gender === "MALE" ? "bg-blue-50 text-blue-600 border-blue-100" :
                      s.gender === "FEMALE" ? "bg-pink-50 text-pink-600 border-pink-100" :
                        "bg-gray-50 text-gray-600 border-gray-100"
                      }`}>
                      {s.gender}
                    </span>
                  </td>
                  <td className="p-4 text-center text-gray-500 text-xs italic">{s.contactNumber || "No info"}</td>
                  <td className="p-4 text-center">
                    <button className="text-gray-400 hover:text-blue-600 p-1">
                      <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    </button>
                  </td>
                </tr>
              ))}
              {students.length === 0 && (
                <tr>
                  <td colSpan={5} className="p-20 text-center text-gray-400 italic">
                    <div className="flex flex-col items-center gap-2">
                      <span className="text-4xl">📂</span>
                      No students enrolled in this class yet.
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="p-20 text-center bg-gray-50 rounded-2xl border border-dashed border-gray-300 text-gray-400 italic">
          Please select a class to view and manage students.
        </div>
      )}

      {/* Add Modal */}
      <Modal
        isOpen={showAddModal}
        onClose={() => setShowAddModal(false)}
        title="Add New Student"
        maxWidth="max-w-2xl"
        footer={
          <div className="flex gap-2">
            <button
              onClick={() => setShowAddModal(false)}
              className="px-6 py-2 rounded-xl border font-medium text-gray-600 hover:bg-gray-50 transition-all"
            >
              Cancel
            </button>
            <button
              onClick={saveStudent}
              disabled={isSaving}
              className="px-8 py-2 rounded-xl bg-blue-600 text-white font-bold shadow-lg hover:bg-blue-700 disabled:bg-gray-400 transition-all"
            >
              {isSaving ? "Enrolling..." : "Enroll Student"}
            </button>
          </div>
        }
      >
        <div className="grid grid-cols-2 gap-5">
          <h3 className="col-span-2 font-bold text-gray-700 border-b pb-2">Personal Information</h3>

          <input
            name="firstName"
            placeholder="First Name *"
            value={studentForm.firstName}
            onChange={updateStudentField}
            className="input-ref"
          />
          <input
            name="lastName"
            placeholder="Last Name"
            value={studentForm.lastName}
            onChange={updateStudentField}
            className="input-ref"
          />

          <select
            name="gender"
            value={studentForm.gender}
            onChange={updateStudentField}
            className="input-ref"
          >
            <option value="">Select Gender *</option>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
          </select>

          <input
            name="admissionNumber"
            placeholder="Admission No *"
            value={studentForm.admissionNumber}
            onChange={updateStudentField}
            className="input-ref"
          />

          <h3 className="col-span-2 font-bold text-gray-700 border-b pb-2 mt-4">Enrollment Details</h3>

          <select
            name="classId"
            value={studentForm.classId}
            onChange={updateStudentField}
            className="input-ref"
          >
            <option value="">Enroll in Class *</option>
            {classes.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} {c.section}
              </option>
            ))}
          </select>

          <div className="p-3 bg-blue-50 rounded-xl border border-blue-100 italic text-xs text-blue-600 font-medium">
            This student will be enrolled for the active session: <span className="font-bold">{currentSession?.name || "None"}</span>
          </div>

          <h3 className="col-span-2 font-bold text-gray-700 border-b pb-2 mt-4">Contact Information</h3>

          <input
            name="contactNumber"
            placeholder="Phone Number"
            value={studentForm.contactNumber}
            onChange={updateStudentField}
            className="input-ref"
          />
          <input
            name="email"
            type="email"
            placeholder="Email Address"
            value={studentForm.email}
            onChange={updateStudentField}
            className="input-ref"
          />
        </div>
      </Modal>
    </div>
  );
}
