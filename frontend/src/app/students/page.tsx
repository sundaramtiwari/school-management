"use client";

import { useEffect, useState } from "react";
import { schoolApi } from "@/lib/schoolApi";
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
};

/* ---------------- Page ---------------- */

export default function StudentsPage() {

  /* ---------------- State ---------------- */

  const [schools, setSchools] = useState<School[]>([]);
  const [classes, setClasses] = useState<SchoolClass[]>([]);

  const [selectedSchool, setSelectedSchool] = useState<number | "">("");
  const [selectedClass, setSelectedClass] = useState<number | "">("");

  const [loadingSchools, setLoadingSchools] = useState(true);
  const [loadingClasses, setLoadingClasses] = useState(false);

  const [students, setStudents] = useState<Student[]>([]);
  const [loadingStudents, setLoadingStudents] = useState(false);

  const [error, setError] = useState("");

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

      const res = await api.get(`/api/classes/by-school/${schoolId}`);

      setClasses(res.data.content || []);

    } catch {
      setError("Failed to load classes");
    } finally {
      setLoadingClasses(false);
    }
  }

  /* ---------------- Load Students --------------*/

  async function loadStudents(classId: number) {
    try {
      setLoadingStudents(true);
      setStudents([]);

      const res = await api.get(`/api/students/by-class/${classId}`);

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


  /* ---------------- UI ---------------- */

  return (
    <div className="space-y-6">

      {/* ---------------- Header ---------------- */}

      <h1 className="text-2xl font-bold">Students</h1>

      {/* ---------------- Filters ---------------- */}

      <div className="bg-white p-4 border rounded flex gap-4 items-center">

        {/* School Dropdown */}

        <div className="w-1/3">

          <label className="block text-sm font-medium mb-1">
            School
          </label>

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

        {/* Class Dropdown */}

        <div className="w-1/3">

          <label className="block text-sm font-medium mb-1">
            Class
          </label>

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

      {loadingSchools && (
        <p className="text-gray-500">Loading schools...</p>
      )}

      {loadingClasses && (
        <p className="text-gray-500">Loading classes...</p>
      )}

      {error && (
        <p className="text-red-600">{error}</p>
      )}

      {/* ---------------- Placeholder ---------------- */}

      {selectedClass && (

        <div className="bg-white border rounded">

          <div className="p-3 border-b font-medium">
            Students
          </div>

          {loadingStudents && (
            <p className="p-4 text-gray-500">Loading students...</p>
          )}

          {!loadingStudents && students.length === 0 && (
            <p className="p-4 text-gray-500">No students found</p>
          )}

          {!loadingStudents && students.length > 0 && (

            <table className="w-full text-sm">

              <thead className="bg-gray-100">
                <tr>
                  <th className="p-3 text-left">Name</th>
                  <th className="p-3 text-center">Admission No</th>
                  <th className="p-3 text-center">Gender</th>
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

                  </tr>

                ))}

              </tbody>

            </table>

          )}

        </div>
      )}

    </div>
  );
}
