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
  gender: string;
  contactNumber: string;
};

/* ---------------- Page ---------------- */

export default function StudentsPage() {

  /* ---------- Filters ---------- */

  const [schools, setSchools] = useState<School[]>([]);
  const [classes, setClasses] = useState<SchoolClass[]>([]);

  const [selectedSchool, setSelectedSchool] = useState<number | "">("");
  const [selectedClass, setSelectedClass] = useState<number | "">("");

  /* ---------- Students ---------- */

  const [students, setStudents] = useState<Student[]>([]);
  const [loadingStudents, setLoadingStudents] = useState(false);

  /* ---------- Loading ---------- */

  const [loadingSchools, setLoadingSchools] = useState(true);
  const [loadingClasses, setLoadingClasses] = useState(false);

  const [error, setError] = useState("");

  /* ---------- Add Modal ---------- */

  const [showAddModal, setShowAddModal] = useState(false);

  const [studentForm, setStudentForm] = useState({
    admissionNumber: "",
    firstName: "",
    lastName: "",
    gender: "",
    contactNumber: "",
    email: "",
    classId: "",
    session: "",
  });

  /* ---------------- Load Schools ---------------- */

  useEffect(() => {
    loadSchools();
  }, []);

  async function loadSchools() {
    try {
      setLoadingSchools(true);

      const res = await schoolApi.list(0, 100);
      setSchools(res.data.content || []);

    } catch {
      setError("Failed to load schools");
    } finally {
      setLoadingSchools(false);
    }
  }

  /* ---------------- Load Classes ---------------- */

  async function loadClasses(schoolId: number) {
    try {
      setLoadingClasses(true);
      setClasses([]);
      setSelectedClass("");
      setStudents([]);

      const res = await api.get(`/api/classes/by-school/${schoolId}`);

      setClasses(res.data.content || []);

    } catch {
      setError("Failed to load classes");
    } finally {
      setLoadingClasses(false);
    }
  }

  /* ---------------- Load Students ---------------- */

  async function loadStudents(classId: number) {
    try {
      setLoadingStudents(true);

      const res = await studentApi.byClass(classId, 0, 20);

      setStudents(res.data.content || []);

    } catch {
      setError("Failed to load students");
    } finally {
      setLoadingStudents(false);
    }
  }

  /* ---------------- Handlers ---------------- */

  function onSchoolChange(e: any) {

    const value = e.target.value;

    setSelectedSchool(value);
    setSelectedClass("");
    setClasses([]);
    setStudents([]);

    if (value) {
      loadClasses(Number(value));
    }
  }

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

  function resetStudentForm() {

    setStudentForm({
      admissionNumber: "",
      firstName: "",
      lastName: "",
      gender: "",
      contactNumber: "",
      email: "",
      classId: "",
      session: "",
    });
  }

  /* ---------------- Save Student ---------------- */

  async function saveStudent() {

    if (!studentForm.firstName) {
      alert("First name required");
      return;
    }

    if (!studentForm.gender) {
      alert("Gender required");
      return;
    }

    if (!studentForm.classId) {
      alert("Select class");
      return;
    }

    if (!studentForm.session) {
      alert("Session required");
      return;
    }

    try {

      // 1. Create student
      const res = await studentApi.create({
        admissionNumber: studentForm.admissionNumber,
        firstName: studentForm.firstName,
        lastName: studentForm.lastName,
        gender: studentForm.gender,
        contactNumber: studentForm.contactNumber,
        email: studentForm.email,
        schoolId: selectedSchool,
      });

      const studentId = res.data.id;

      // 2. Enroll student
      await studentApi.enroll({
        studentId,
        classId: studentForm.classId,
        session: studentForm.session,
      });

      alert("Student added successfully");

      setShowAddModal(false);
      resetStudentForm();

      if (selectedClass) {
        loadStudents(Number(selectedClass));
      }

    } catch {
      alert("Failed to save student");
    }
  }

  /* ---------------- UI ---------------- */

  return (
    <div className="space-y-6">

      {/* ---------------- Header ---------------- */}

      <div className="flex justify-between items-center">

        <h1 className="text-2xl font-bold">Students</h1>

        <button
          disabled={!selectedClass}
          onClick={() => setShowAddModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded disabled:bg-gray-400"
        >
          + Add Student
        </button>

      </div>

      {/* ---------------- Filters ---------------- */}

      <div className="bg-white p-4 border rounded flex gap-4 items-center">

        {/* School */}

        <div className="w-1/3">

          <label className="block text-sm mb-1">School</label>

          <select
            value={selectedSchool}
            onChange={onSchoolChange}
            className="w-full border rounded px-3 py-2"
          >
            <option value="">Select School</option>

            {schools.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>

        </div>

        {/* Class */}

        <div className="w-1/3">

          <label className="block text-sm mb-1">Class</label>

          <select
            value={selectedClass}
            onChange={onClassChange}
            disabled={!selectedSchool || loadingClasses}
            className="w-full border rounded px-3 py-2 disabled:bg-gray-100"
          >
            <option value="">Select Class</option>

            {classes.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} {c.section} ({c.session})
              </option>
            ))}
          </select>

        </div>

      </div>

      {/* ---------------- Status ---------------- */}

      {loadingSchools && <p>Loading schools...</p>}
      {loadingClasses && <p>Loading classes...</p>}
      {loadingStudents && <p>Loading students...</p>}

      {error && <p className="text-red-600">{error}</p>}

      {/* ---------------- Students Table ---------------- */}

      {selectedClass && !loadingStudents && (

        <div className="bg-white border rounded">

          <table className="w-full text-sm">

            <thead className="bg-gray-100">
              <tr>
                <th className="p-3 text-left">Name</th>
                <th className="p-3">Admission No</th>
                <th className="p-3">Gender</th>
                <th className="p-3">Phone</th>
              </tr>
            </thead>

            <tbody>

              {students.map((s) => (

                <tr key={s.id} className="border-t">

                  <td className="p-3">
                    {s.firstName} {s.lastName}
                  </td>

                  <td className="p-3 text-center">
                    {s.admissionNumber}
                  </td>

                  <td className="p-3 text-center">
                    {s.gender}
                  </td>

                  <td className="p-3 text-center">
                    {s.contactNumber}
                  </td>

                </tr>

              ))}

              {students.length === 0 && (

                <tr>
                  <td colSpan={4} className="p-4 text-center text-gray-500">
                    No students found
                  </td>
                </tr>

              )}

            </tbody>

          </table>

        </div>
      )}

      {/* ---------------- Add Student Modal ---------------- */}

      {showAddModal && (

        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">

          <div className="bg-white p-6 rounded w-[600px] space-y-4">

            <h2 className="font-semibold text-lg">Add Student</h2>

            <div className="grid grid-cols-2 gap-4">

              <input
                name="admissionNumber"
                placeholder="Admission No"
                value={studentForm.admissionNumber}
                onChange={updateStudentField}
                className="input"
              />

              <input
                name="firstName"
                placeholder="First Name *"
                value={studentForm.firstName}
                onChange={updateStudentField}
                className="input"
              />

              <input
                name="lastName"
                placeholder="Last Name"
                value={studentForm.lastName}
                onChange={updateStudentField}
                className="input"
              />

              <select
                name="gender"
                value={studentForm.gender}
                onChange={updateStudentField}
                className="input"
              >
                <option value="">Gender *</option>
                <option>Male</option>
                <option>Female</option>
              </select>

              <input
                name="contactNumber"
                placeholder="Phone"
                value={studentForm.contactNumber}
                onChange={updateStudentField}
                className="input"
              />

              <input
                name="email"
                placeholder="Email"
                value={studentForm.email}
                onChange={updateStudentField}
                className="input"
              />

              <select
                name="classId"
                value={studentForm.classId}
                onChange={updateStudentField}
                className="input col-span-2"
              >
                <option value="">Select Class *</option>

                {classes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} {c.section}
                  </option>
                ))}

              </select>

              <input
                name="session"
                placeholder="Session (2025-26) *"
                value={studentForm.session}
                onChange={updateStudentField}
                className="input col-span-2"
              />

            </div>

            <div className="flex justify-end gap-3 pt-4">

              <button
                onClick={() => {
                  setShowAddModal(false);
                  resetStudentForm();
                }}
                className="px-4 py-2 border rounded"
              >
                Cancel
              </button>

              <button
                onClick={saveStudent}
                className="bg-blue-600 text-white px-5 py-2 rounded"
              >
                Save
              </button>

            </div>

          </div>

        </div>
      )}

    </div>
  );
}
