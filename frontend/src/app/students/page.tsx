"use client";

import { useCallback, useEffect, useMemo, useState, type ChangeEvent } from "react";
import { studentApi } from "@/lib/studentApi";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";
import { useSession } from "@/context/SessionContext";
import Modal from "@/components/ui/Modal";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { useAuth } from "@/context/AuthContext";
import GuardianFormSection, { GuardianFormValue } from "@/components/students/GuardianFormSection";
import PromotionModal from "@/components/promotion/PromotionModal";

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

type LedgerEntry = {
  sessionId: number;
  sessionName: string;
  totalAssigned: number | string;
  totalDiscount: number | string;
  totalFunding: number | string;
  totalLateFee: number | string;
  totalPaid: number | string;
  totalPending: number | string;
};

/* ---------------- Page ---------------- */

export default function StudentsPage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const { currentSession } = useSession();

  const role = user?.role?.toUpperCase();
  const canAddStudent = role === "SCHOOL_ADMIN" || role === "SUPER_ADMIN" || role === "PLATFORM_ADMIN";
  const canPromoteStudents = role === "SCHOOL_ADMIN" || role === "SUPER_ADMIN" || role === "PLATFORM_ADMIN";

  /* ---------- Filters ---------- */

  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [selectedClass, setSelectedClass] = useState<number | "">("");

  /* ---------- Students ---------- */

  const [students, setStudents] = useState<Student[]>([]);
  const [loadingStudents, setLoadingStudents] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [showPromotionModal, setShowPromotionModal] = useState(false);

  /* ---------- Loading ---------- */

  const [loadingClasses, setLoadingClasses] = useState(true);
  /* ---------- Form State ---------- */

  const [showAddModal, setShowAddModal] = useState(false);
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [profileStudent, setProfileStudent] = useState<Student | null>(null);
  const [profileTab, setProfileTab] = useState<"overview" | "ledger">("overview");
  const [ledgerLoading, setLedgerLoading] = useState(false);
  const [ledgerData, setLedgerData] = useState<LedgerEntry[]>([]);
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
    guardians: [] as GuardianFormValue[],
    // Funding / Sponsorship
    fundingType: "NONE" as "NONE" | "FULL" | "PARTIAL",
    fundingMode: "FIXED_AMOUNT" as "FIXED_AMOUNT" | "PERCENTAGE",
    fundingValue: "0",
    fundingValidFrom: "",
    fundingValidTo: "",
  });

  /* ---------------- Init ---------------- */

  const selectedStudents = useMemo(
    () => students.filter((s) => selectedIds.has(s.id)),
    [students, selectedIds]
  );

  const loadClasses = useCallback(async () => {
    try {
      setLoadingClasses(true);
      const res = await api.get("/api/classes/mine");
      setClasses(res.data.content || []);
    } catch {
      showToast("Error loading classes", "error");
    } finally {
      setLoadingClasses(false);
    }
  }, [showToast]);

  const loadStudents = useCallback(async (classId: number) => {
    try {
      setLoadingStudents(true);
      const res = await studentApi.byClass(classId, 0, 50);
      setStudents(res.data.content || []);
      setSelectedIds(new Set());
    } catch {
      showToast("Failed to load students", "error");
    } finally {
      setLoadingStudents(false);
    }
  }, [showToast]);

  const loadStudentLedger = useCallback(async (studentId: number) => {
    try {
      setLedgerLoading(true);
      const res = await api.get(`/api/students/${studentId}/ledger-summary`);
      setLedgerData(Array.isArray(res.data) ? res.data : []);
    } catch {
      showToast("Failed to load financial ledger", "error");
      setLedgerData([]);
    } finally {
      setLedgerLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    loadClasses();
  }, [currentSession, loadClasses]);

  useEffect(() => {
    if (!showProfileModal || profileTab !== "ledger" || !profileStudent?.id) return;
    void loadStudentLedger(profileStudent.id);
  }, [showProfileModal, profileTab, profileStudent?.id, loadStudentLedger]);

  /* ---------------- Handlers ---------------- */

  const onClassChange = useCallback((e: ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value;
    const classValue = value ? Number(value) : "";
    setSelectedClass(classValue);
    setStudents([]);
    setSelectedIds(new Set());
    if (classValue) {
      void loadStudents(Number(classValue));
    }
  }, [loadStudents]);

  const toggleStudentSelection = useCallback((studentId: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(studentId)) next.delete(studentId);
      else next.add(studentId);
      return next;
    });
  }, []);

  const toggleSelectAll = useCallback(() => {
    setSelectedIds((prev) => {
      if (students.length > 0 && prev.size === students.length) return new Set();
      return new Set(students.map((s) => s.id));
    });
  }, [students]);

  const openProfile = useCallback((student: Student) => {
    setProfileStudent(student);
    setProfileTab("overview");
    setLedgerData([]);
    setShowProfileModal(true);
  }, []);

  const closeProfile = useCallback(() => {
    setShowProfileModal(false);
    setProfileStudent(null);
    setLedgerData([]);
    setProfileTab("overview");
  }, []);

  const onPromotionSuccess = useCallback(() => {
    setShowPromotionModal(false);
    setSelectedIds(new Set());
    if (selectedClass) {
      void loadStudents(Number(selectedClass));
    }
  }, [selectedClass, loadStudents]);

  function updateStudentField(e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) {
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

    if (studentForm.guardians.length === 0) {
      showToast("At least one guardian is required", "warning");
      return;
    }

    if (!studentForm.guardians.some(g => g.primaryGuardian)) {
      showToast("Please designate a primary guardian", "warning");
      return;
    }

    try {
      setIsSaving(true);

      const { classId, ...registerData } = studentForm;

      const res = await studentApi.create({
        ...registerData,
        previousYearOfPassing: registerData.previousYearOfPassing ? Number(registerData.previousYearOfPassing) : null,
        guardians: studentForm.guardians
      });

      const studentId = res.data.id;
      await studentApi.enroll({
        studentId,
        classId,
        sessionId: currentSession.id,
      });

      // --- Save Funding arrangement if applicable ---
      if (studentForm.fundingType !== "NONE") {
        await api.post("/api/fees/funding", {
          studentId,
          sessionId: currentSession.id,
          coverageType: studentForm.fundingType,
          coverageMode: studentForm.fundingMode,
          coverageValue: Number(studentForm.fundingValue),
          validFrom: studentForm.fundingValidFrom || null,
          validTo: studentForm.fundingValidTo || null,
        });
      }

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
        guardians: [],
        fundingType: "NONE",
        fundingMode: "FIXED_AMOUNT",
        fundingValue: "0",
        fundingValidFrom: "",
        fundingValidTo: "",
      });

      if (Number(classId) === Number(selectedClass)) {
        void loadStudents(Number(selectedClass));
      }
    } catch (err: unknown) {
      const msg = (typeof err === "object" && err !== null && "response" in err)
        ? (err as { response?: { data?: { message?: string } | string }; message?: string }).response?.data &&
          typeof (err as { response?: { data?: { message?: string } | string } }).response?.data === "object"
          ? ((err as { response?: { data?: { message?: string } } }).response?.data?.message || "Unknown error")
          : ((err as { response?: { data?: string }; message?: string }).response?.data || (err as { message?: string }).message || "Unknown error")
        : "Unknown error";
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
          <div className="flex gap-2">
            {canPromoteStudents && (
              <button
                onClick={() => setShowPromotionModal(true)}
                disabled={selectedStudents.length === 0}
                className="bg-emerald-600 text-white px-6 py-2.5 rounded-xl font-bold shadow-lg hover:bg-emerald-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-all"
              >
                Promote Students
              </button>
            )}
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
                <th className="p-4 text-center w-12">
                  <input
                    type="checkbox"
                    checked={students.length > 0 && selectedIds.size === students.length}
                    onChange={toggleSelectAll}
                  />
                </th>
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
                  <td className="p-4 text-center">
                    <input
                      type="checkbox"
                      checked={selectedIds.has(s.id)}
                      onChange={() => toggleStudentSelection(s.id)}
                    />
                  </td>
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
                    <button
                      onClick={() => openProfile(s)}
                      className="text-gray-400 hover:text-blue-600 p-1"
                    >
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
                  <td colSpan={6} className="p-20 text-center text-gray-400 italic bg-gray-50/30">
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

          {/* Section 6: Funding / Sponsorship (New) */}
          {canAddStudent && (
            <section className="space-y-4 bg-blue-50/30 p-4 rounded-2xl border border-blue-100">
              <h3 className="text-sm font-bold text-blue-600 uppercase tracking-wider border-b border-blue-100 pb-2 flex items-center gap-2">
                <span className="bg-blue-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-[10px]">6</span>
                Funding / Sponsorship
              </h3>
              <div className="grid grid-cols-3 gap-4">
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-gray-400 ml-1">Coverage Type</label>
                  <select
                    name="fundingType"
                    value={studentForm.fundingType}
                    onChange={updateStudentField}
                    className="input-ref"
                  >
                    <option value="NONE">None / Self-Paid</option>
                    <option value="FULL">Full Scholarship (100%)</option>
                    <option value="PARTIAL">Partial Sponsorship</option>
                  </select>
                </div>

                {studentForm.fundingType === "PARTIAL" && (
                  <>
                    <div className="space-y-1">
                      <label className="text-[10px] font-bold text-gray-400 ml-1">Mode</label>
                      <select
                        name="fundingMode"
                        value={studentForm.fundingMode}
                        onChange={updateStudentField}
                        className="input-ref"
                      >
                        <option value="FIXED_AMOUNT">Fixed Amount ($)</option>
                        <option value="PERCENTAGE">Percentage (%)</option>
                      </select>
                    </div>
                    <div className="space-y-1">
                      <label className="text-[10px] font-bold text-gray-400 ml-1">Value</label>
                      <input
                        name="fundingValue"
                        type="number"
                        placeholder="0.00"
                        value={studentForm.fundingValue}
                        onChange={updateStudentField}
                        className="input-ref"
                      />
                    </div>
                  </>
                )}

                {studentForm.fundingType !== "NONE" && (
                  <>
                    <div className="space-y-1">
                      <label className="text-[10px] font-bold text-gray-400 ml-1">Valid From</label>
                      <input
                        name="fundingValidFrom"
                        type="date"
                        value={studentForm.fundingValidFrom}
                        onChange={updateStudentField}
                        className="input-ref"
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="text-[10px] font-bold text-gray-400 ml-1">Valid To</label>
                      <input
                        name="fundingValidTo"
                        type="date"
                        value={studentForm.fundingValidTo}
                        onChange={updateStudentField}
                        className="input-ref"
                      />
                    </div>
                  </>
                )}
              </div>
            </section>
          )}
          onChange={updateStudentField}
          className="input-ref"
                />
        </div>
    </div>
          </section >

    {/* Section 6: Remarks */ }
    < section className = "space-y-4" >
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
          </section >

    {/* Section 7: Guardians */ }
    < section className = "space-y-4 mb-4" >
            <h3 className="text-sm font-bold text-blue-600 uppercase tracking-wider border-b pb-2 flex items-center gap-2">
              <span className="bg-blue-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-[10px]">7</span>
              Guardian Information
            </h3>
            <GuardianFormSection
              guardians={studentForm.guardians}
              onChange={(newGuardians) => setStudentForm({ ...studentForm, guardians: newGuardians })}
            />
          </section >

        </div >
      </Modal >

      <PromotionModal
        selectedStudents={selectedStudents}
        isOpen={showPromotionModal}
        onClose={() => setShowPromotionModal(false)}
        onSuccess={onPromotionSuccess}
      />

      <Modal
        isOpen={showProfileModal}
        onClose={closeProfile}
        title={profileStudent ? `${profileStudent.firstName} ${profileStudent.lastName}` : "Student Profile"}
        maxWidth="max-w-3xl"
      >
        <div className="space-y-4">
          <div className="flex gap-2 border-b pb-2">
            <button
              onClick={() => setProfileTab("overview")}
              className={`px-3 py-1.5 rounded-lg text-sm font-semibold ${profileTab === "overview" ? "bg-blue-600 text-white" : "bg-gray-100 text-gray-700"}`}
            >
              Overview
            </button>
            <button
              onClick={() => setProfileTab("ledger")}
              className={`px-3 py-1.5 rounded-lg text-sm font-semibold ${profileTab === "ledger" ? "bg-blue-600 text-white" : "bg-gray-100 text-gray-700"}`}
            >
              Financial Ledger
            </button>
          </div>

          {profileTab === "overview" && profileStudent && (
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div><span className="text-gray-500">Admission:</span> {profileStudent.admissionNumber || "-"}</div>
              <div><span className="text-gray-500">Gender:</span> {profileStudent.gender || "-"}</div>
              <div><span className="text-gray-500">Contact:</span> {profileStudent.contactNumber || "-"}</div>
            </div>
          )}

          {profileTab === "ledger" && (
            <div className="space-y-3">
              {ledgerLoading ? (
                <TableSkeleton rows={3} cols={4} />
              ) : ledgerData.length === 0 ? (
                <div className="text-sm text-gray-500">No ledger entries found.</div>
              ) : (
                ledgerData.map((entry) => (
                  <details key={`${entry.sessionId}-${entry.sessionName}`} className="border rounded-xl p-3">
                    <summary className="font-semibold cursor-pointer">{entry.sessionName}</summary>
                    <div className="mt-3 grid grid-cols-2 lg:grid-cols-4 gap-4 text-[11px] font-bold tracking-tight">
                      <div className="bg-gray-50 p-2 rounded-lg border border-gray-100">
                        <span className="block text-gray-400 uppercase text-[9px] mb-1">Total Assigned</span>
                        <span className="text-gray-800">₹{Number(entry.totalAssigned).toLocaleString()}</span>
                      </div>
                      <div className="bg-blue-50 p-2 rounded-lg border border-blue-100">
                        <span className="block text-blue-400 uppercase text-[9px] mb-1">Discount (-)</span>
                        <span className="text-blue-600">₹{Number(entry.totalDiscount).toLocaleString()}</span>
                      </div>
                      <div className="bg-indigo-50 p-2 rounded-lg border border-indigo-100">
                        <span className="block text-indigo-400 uppercase text-[9px] mb-1">Sponsor Covered (-)</span>
                        <span className="text-indigo-600">₹{Number(entry.totalFunding).toLocaleString()}</span>
                      </div>
                      <div className="bg-orange-50 p-2 rounded-lg border border-orange-100">
                        <span className="block text-orange-400 uppercase text-[9px] mb-1">Late Fee (+)</span>
                        <span className="text-orange-600">₹{Number(entry.totalLateFee).toLocaleString()}</span>
                      </div>
                      <div className="bg-green-50 p-2 rounded-lg border border-green-100">
                        <span className="block text-green-400 uppercase text-[9px] mb-1">Total Paid</span>
                        <span className="text-green-600">₹{Number(entry.totalPaid).toLocaleString()}</span>
                      </div>
                      <div className="bg-gray-900 p-2 rounded-lg col-span-2">
                        <span className="block text-gray-400 uppercase text-[9px] mb-1">Current Balance</span>
                        <span className="text-white">
                          {Number(entry.totalPending) > 0 ? `₹${Number(entry.totalPending).toLocaleString()}` : "SETTLED"}
                        </span>
                      </div>
                    </div>
                  </details>
                ))
              )}
            </div>
          )}
        </div>
      </Modal>
    </div >
  );
}
