"use client";

import { useEffect, useState } from "react";
import { studentApi } from "@/lib/studentApi";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";
import { useSession } from "@/context/SessionContext";
import Modal from "@/components/ui/Modal";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { useAuth } from "@/context/AuthContext";

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
  const { user } = useAuth();
  const { showToast } = useToast();
  const { currentSession } = useSession();

  const canAddStudent = user?.role === "SCHOOL_ADMIN" || user?.role === "SUPER_ADMIN" || user?.role === "PLATFORM_ADMIN";

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
    dob: "",
    gender: "",
    pen: "",
    aadharNumber: "",
    religion: "",
    caste: "",
    category: "",
    address: "",
    city: "",
    state: "",
    pincode: "",
    contactNumber: "",
    email: "",
    bloodGroup: "",
    dateOfAdmission: new Date().toISOString().split('T')[0],
    remarks: "",
    // Previous School
    previousSchoolName: "",
    previousSchoolBoard: "",
    previousClass: "",
    previousYearOfPassing: "",
    transferCertificateNumber: "",
    previousSchoolAddress: "",
    previousSchoolContact: "",
    reasonForLeavingPreviousSchool: "",
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

      const { classId, ...registerData } = studentForm;

      const res = await studentApi.create({
        ...registerData,
        previousYearOfPassing: registerData.previousYearOfPassing ? Number(registerData.previousYearOfPassing) : null
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
        dob: "",
        gender: "",
        pen: "",
        aadharNumber: "",
        religion: "",
        caste: "",
        category: "",
        address: "",
        city: "",
        state: "",
        pincode: "",
        contactNumber: "",
        email: "",
        bloodGroup: "",
        dateOfAdmission: new Date().toISOString().split('T')[0],
        remarks: "",
        previousSchoolName: "",
        previousSchoolBoard: "",
        previousClass: "",
        previousYearOfPassing: "",
        transferCertificateNumber: "",
        previousSchoolAddress: "",
        previousSchoolContact: "",
        reasonForLeavingPreviousSchool: "",
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

        {canAddStudent && (
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
        )}
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
                  <td colSpan={5} className="p-20 text-center text-gray-400 italic bg-gray-50/30">
                    <div className="flex flex-col items-center gap-3">
                      <span className="text-4xl">📂</span>
                      <div>
                        <p className="font-bold text-gray-800">Class is Empty</p>
                        <p className="text-sm">No students have been enrolled in this class for the current session.</p>
                      </div>
                      {canAddStudent && (
                        <button
                          onClick={() => {
                            if (selectedClass) {
                              setStudentForm(prev => ({ ...prev, classId: selectedClass.toString() }));
                            }
                            setShowAddModal(true);
                          }}
                          className="mt-2 bg-blue-50 text-blue-600 px-6 py-2 rounded-xl font-bold hover:bg-blue-100 transition-all border border-blue-200"
                        >
                          Enroll First Student →
                        </button>
                      )}
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
        title="Enroll New Student"
        maxWidth="max-w-4xl"
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
        <div className="flex flex-col gap-8 max-h-[70vh] overflow-y-auto px-1 pr-4 custom-scrollbar">

          {/* Section 1: Admission & Enrollment */}
          <section className="space-y-4">
            <h3 className="text-sm font-bold text-blue-600 uppercase tracking-wider border-b pb-2 flex items-center gap-2">
              <span className="bg-blue-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-[10px]">1</span>
              Enrollment Details
            </h3>
            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Admission Number *</label>
                <input
                  name="admissionNumber"
                  placeholder="e.g. ADM-2025-001"
                  value={studentForm.admissionNumber}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Date of Admission</label>
                <input
                  name="dateOfAdmission"
                  type="date"
                  value={studentForm.dateOfAdmission}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Assign Class *</label>
                <select
                  name="classId"
                  value={studentForm.classId}
                  onChange={updateStudentField}
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
          </section>

          {/* Section 2: Personal Information */}
          <section className="space-y-4">
            <h3 className="text-sm font-bold text-blue-600 uppercase tracking-wider border-b pb-2 flex items-center gap-2">
              <span className="bg-blue-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-[10px]">2</span>
              Personal Information
            </h3>
            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">First Name *</label>
                <input
                  name="firstName"
                  placeholder="First name"
                  value={studentForm.firstName}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Last Name</label>
                <input
                  name="lastName"
                  placeholder="Last name"
                  value={studentForm.lastName}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Gender *</label>
                <select
                  name="gender"
                  value={studentForm.gender}
                  onChange={updateStudentField}
                  className="input-ref"
                >
                  <option value="">Select</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Date of Birth</label>
                <input
                  name="dob"
                  type="date"
                  value={studentForm.dob}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Blood Group</label>
                <select
                  name="bloodGroup"
                  value={studentForm.bloodGroup}
                  onChange={updateStudentField}
                  className="input-ref"
                >
                  <option value="">Unknown</option>
                  <option value="A+">A+</option>
                  <option value="A-">A-</option>
                  <option value="B+">B+</option>
                  <option value="B-">B-</option>
                  <option value="O+">O+</option>
                  <option value="O-">O-</option>
                  <option value="AB+">AB+</option>
                  <option value="AB-">AB-</option>
                </select>
              </div>
            </div>
          </section>

          {/* Section 3: Identity & Demographics */}
          <section className="space-y-4">
            <h3 className="text-sm font-bold text-blue-600 uppercase tracking-wider border-b pb-2 flex items-center gap-2">
              <span className="bg-blue-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-[10px]">3</span>
              Identity & Demographics
            </h3>
            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Aadhar Number</label>
                <input
                  name="aadharNumber"
                  placeholder="12-digit number"
                  value={studentForm.aadharNumber}
                  onChange={updateStudentField}
                  className="input-ref font-mono"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">PEN (Permanent Edu Number)</label>
                <input
                  name="pen"
                  placeholder="PEN"
                  value={studentForm.pen}
                  onChange={updateStudentField}
                  className="input-ref font-mono"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Religion</label>
                <input
                  name="religion"
                  placeholder="Religion"
                  value={studentForm.religion}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Category</label>
                <select
                  name="category"
                  value={studentForm.category}
                  onChange={updateStudentField}
                  className="input-ref"
                >
                  <option value="">Select</option>
                  <option value="GENERAL">General</option>
                  <option value="OBC">OBC</option>
                  <option value="SC">SC</option>
                  <option value="ST">ST</option>
                  <option value="OTHERS">Others</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Caste</label>
                <input
                  name="caste"
                  placeholder="Caste"
                  value={studentForm.caste}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
            </div>
          </section>

          {/* Section 4: Contact & Address */}
          <section className="space-y-4">
            <h3 className="text-sm font-bold text-blue-600 uppercase tracking-wider border-b pb-2 flex items-center gap-2">
              <span className="bg-blue-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-[10px]">4</span>
              Contact & Address
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Contact Number</label>
                <input
                  name="contactNumber"
                  placeholder="Phone No"
                  value={studentForm.contactNumber}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Email Address</label>
                <input
                  name="email"
                  type="email"
                  placeholder="Email"
                  value={studentForm.email}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
              <div className="col-span-2 space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Full Address</label>
                <textarea
                  name="address"
                  placeholder="Building, Street, Area..."
                  value={studentForm.address}
                  onChange={updateStudentField}
                  className="input-ref min-h-[80px]"
                />
              </div>
              <div className="grid grid-cols-3 gap-4 col-span-2">
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-gray-400 ml-1">City</label>
                  <input
                    name="city"
                    placeholder="City"
                    value={studentForm.city}
                    onChange={updateStudentField}
                    className="input-ref"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-gray-400 ml-1">State</label>
                  <input
                    name="state"
                    placeholder="State"
                    value={studentForm.state}
                    onChange={updateStudentField}
                    className="input-ref"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-gray-400 ml-1">Pincode</label>
                  <input
                    name="pincode"
                    placeholder="Pincode"
                    value={studentForm.pincode}
                    onChange={updateStudentField}
                    className="input-ref"
                  />
                </div>
              </div>
            </div>
          </section>

          {/* Section 5: Previous School Details */}
          <section className="space-y-4">
            <h3 className="text-sm font-bold text-blue-600 uppercase tracking-wider border-b pb-2 flex items-center gap-2">
              <span className="bg-blue-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-[10px]">5</span>
              Previous School History
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Previous School Name</label>
                <input
                  name="previousSchoolName"
                  placeholder="School Name"
                  value={studentForm.previousSchoolName}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Previous Class</label>
                <input
                  name="previousClass"
                  placeholder="e.g. 5th"
                  value={studentForm.previousClass}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Board</label>
                <input
                  name="previousSchoolBoard"
                  placeholder="e.g. CBSE / State"
                  value={studentForm.previousSchoolBoard}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Year of Passing</label>
                <input
                  name="previousYearOfPassing"
                  type="number"
                  placeholder="YYYY"
                  value={studentForm.previousYearOfPassing}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">TC Number</label>
                <input
                  name="transferCertificateNumber"
                  placeholder="TC No"
                  value={studentForm.transferCertificateNumber}
                  onChange={updateStudentField}
                  className="input-ref font-mono"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-gray-400 ml-1">Reason for Leaving</label>
                <input
                  name="reasonForLeavingPreviousSchool"
                  placeholder="Reason"
                  value={studentForm.reasonForLeavingPreviousSchool}
                  onChange={updateStudentField}
                  className="input-ref"
                />
              </div>
            </div>
          </section>

          {/* Section 6: Remarks */}
          <section className="space-y-4 mb-4">
            <h3 className="text-sm font-bold text-blue-600 uppercase tracking-wider border-b pb-2 flex items-center gap-2">
              <span className="bg-blue-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-[10px]">6</span>
              Additional Remarks
            </h3>
            <textarea
              name="remarks"
              placeholder="Any additional notes..."
              value={studentForm.remarks}
              onChange={updateStudentField}
              className="input-ref min-h-[60px]"
            />
          </section>

        </div>
      </Modal>
    </div>
  );
}
