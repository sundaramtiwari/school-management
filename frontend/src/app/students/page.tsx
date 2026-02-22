"use client";

import { useCallback, useEffect, useMemo, useState, type ChangeEvent } from "react";
import axios from "axios";
import { studentApi } from "@/lib/studentApi";
import { api } from "@/lib/api";
import { getErrorMessage } from "@/lib/error";
import { canAddStudent, canEditStudent, canPromoteStudents } from "@/lib/permissions";
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
  dob?: string;
  pen?: string;
  aadharNumber?: string;
  religion?: string;
  caste?: string;
  category?: string;
  address?: string;
  city?: string;
  state?: string;
  pincode?: string;
  contactNumber: string;
  email?: string;
  bloodGroup?: string;
  dateOfAdmission?: string;
  remarks?: string;
  previousSchoolName?: string;
  previousSchoolBoard?: string;
  previousClass?: string;
  previousYearOfPassing?: number;
  transferCertificateNumber?: string;
  previousSchoolAddress?: string;
  previousSchoolContact?: string;
  reasonForLeavingPreviousSchool?: string;
  guardians?: GuardianFormValue[];
  currentStatus?: "ENROLLED" | "FAILED" | "LEFT";
};

type LedgerEntry = {
  sessionId: number;
  sessionName: string;
  active?: boolean;
  totalAssigned: number | string;
  totalDiscount: number | string;
  totalFunding: number | string;
  totalLateFee: number | string;
  totalPaid: number | string;
  totalPending: number | string;
};

type StudentEnrollmentDto = {
  id: number;
  studentId: number;
  classId: number;
  section: string;
  sessionId: number;
  rollNumber: number;
  enrollmentDate: string;
  active: boolean;
  remarks: string;
};

type PromotionRecordDto = {
  id: number;
  studentId: number;
  sourceSessionId: number;
  sourceSessionName: string;
  targetSessionId: number;
  targetSessionName: string;
  sourceClassId: number;
  sourceClassName: string;
  targetClassId: number;
  targetClassName: string;
  promotionType: "PROMOTED" | "DEMOTED" | "GRADUATED";
  remarks?: string;
  promotedBy: string;
  promotedAt: string;
};

type StudentFundingArrangement = {
  id: number;
  studentId: number;
  sessionId: number;
  coverageType: "NONE" | "FULL" | "PARTIAL";
  coverageMode: "FIXED_AMOUNT" | "PERCENTAGE";
  coverageValue: number;
  validFrom: string | null;
  validTo: string | null;
  active: boolean;
};

/* ---------------- Page ---------------- */

export default function StudentsPage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const { currentSession, sessions } = useSession();

  const canUserAddStudent = canAddStudent(user?.role);
  const canUserPromoteStudents = canPromoteStudents(user?.role);
  const canUserEditStudent = canEditStudent(user?.role);

  /* ---------- Filters ---------- */

  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [selectedClass, setSelectedClass] = useState<number | "">("");

  /* ---------- Students ---------- */

  const [students, setStudents] = useState<Student[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("asc");
  const [searchTerm, setSearchTerm] = useState("");
  const [loadingStudents, setLoadingStudents] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [showPromotionModal, setShowPromotionModal] = useState(false);

  /* ---------- Loading ---------- */

  const [loadingClasses, setLoadingClasses] = useState(true);
  /* ---------- Form State ---------- */

  const [showAddModal, setShowAddModal] = useState(false);
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [profileStudent, setProfileStudent] = useState<Student | null>(null);
  const [loadingProfile, setLoadingProfile] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [profileTab, setProfileTab] = useState<"overview" | "ledger" | "enrollments" | "promotions" | "funding">("overview");
  const [ledgerLoading, setLedgerLoading] = useState(false);
  const [ledgerData, setLedgerData] = useState<LedgerEntry[]>([]);

  const [enrollmentsLoading, setEnrollmentsLoading] = useState(false);
  const [enrollmentsData, setEnrollmentsData] = useState<StudentEnrollmentDto[]>([]);

  const [promotionsLoading, setPromotionsLoading] = useState(false);
  const [promotionsData, setPromotionsData] = useState<PromotionRecordDto[]>([]);

  const [fundingLoading, setFundingLoading] = useState(false);
  const [fundingData, setFundingData] = useState<StudentFundingArrangement | null>(null);
  const [fundingHistory, setFundingHistory] = useState<StudentFundingArrangement[]>([]);
  const [editingFunding, setEditingFunding] = useState(false);
  const [fundingForm, setFundingForm] = useState({
    coverageType: "NONE" as "NONE" | "FULL" | "PARTIAL",
    coverageMode: "FIXED_AMOUNT" as "FIXED_AMOUNT" | "PERCENTAGE",
    coverageValue: "0",
    validFrom: "",
    validTo: "",
  });

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
  const filteredStudents = useMemo(() => {
    let list = [...students];

    if (searchTerm.trim()) {
      const normalizedSearch = searchTerm.toLowerCase();
      list = list.filter((s) =>
        `${s.firstName} ${s.lastName}`.toLowerCase().includes(normalizedSearch)
      );
    }

    list.sort((a, b) => {
      const nameA = `${a.firstName} ${a.lastName}`.toLowerCase();
      const nameB = `${b.firstName} ${b.lastName}`.toLowerCase();
      return sortOrder === "asc"
        ? nameA.localeCompare(nameB)
        : nameB.localeCompare(nameA);
    });

    return list;
  }, [students, searchTerm, sortOrder]);
  const allFilteredSelected = filteredStudents.length > 0 && filteredStudents.every((s) => selectedIds.has(s.id));

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

  const loadStudents = useCallback(async (classId: number, page = 0) => {
    try {
      setLoadingStudents(true);
      const res = await studentApi.byClass(classId, page, 50);
      setStudents(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
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

  const loadEnrollments = useCallback(async (studentId: number) => {
    try {
      setEnrollmentsLoading(true);
      const res = await studentApi.getEnrollmentHistory(studentId);
      setEnrollmentsData(Array.isArray(res.data) ? res.data : []);
    } catch {
      showToast("Failed to load enrollments history", "error");
      setEnrollmentsData([]);
    } finally {
      setEnrollmentsLoading(false);
    }
  }, [showToast]);

  const loadPromotions = useCallback(async (studentId: number) => {
    try {
      setPromotionsLoading(true);
      const res = await studentApi.getPromotionHistory(studentId);
      setPromotionsData(Array.isArray(res.data) ? res.data : []);
    } catch {
      showToast("Failed to load promotion history", "error");
      setPromotionsData([]);
    } finally {
      setPromotionsLoading(false);
    }
  }, [showToast]);

  const loadFunding = useCallback(async (studentId: number, sessionId: number) => {
    try {
      setFundingLoading(true);
      setEditingFunding(false);
      try {
        const res = await api.get(`/api/fees/funding/student/${studentId}/session/${sessionId}`);
        const data = res.data;
        setFundingData(data);
        setFundingForm({
          coverageType: data.coverageType,
          coverageMode: data.coverageMode,
          coverageValue: data.coverageValue ? data.coverageValue.toString() : "0",
          validFrom: data.validFrom || "",
          validTo: data.validTo || "",
        });
      } catch {
        setFundingData(null);
        setFundingForm({
          coverageType: "NONE",
          coverageMode: "FIXED_AMOUNT",
          coverageValue: "0",
          validFrom: "",
          validTo: "",
        });
      }

      try {
        const histRes = await api.get(`/api/fees/funding/student/${studentId}`);
        setFundingHistory(Array.isArray(histRes.data) ? histRes.data : []);
      } catch {
        setFundingHistory([]);
      }
    } catch {
      showToast("Failed to load funding arrangement", "error");
    } finally {
      setFundingLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    loadClasses();
  }, [currentSession, loadClasses]);

  useEffect(() => {
    if (!showProfileModal || !profileStudent?.id) return;

    if (profileTab === "ledger") {
      void loadStudentLedger(profileStudent.id);
    } else if (profileTab === "enrollments") {
      void loadEnrollments(profileStudent.id);
    } else if (profileTab === "promotions") {
      void loadPromotions(profileStudent.id);
    } else if (profileTab === "funding" && currentSession) {
      void loadFunding(profileStudent.id, currentSession.id);
    }
  }, [showProfileModal, profileTab, profileStudent?.id, currentSession?.id, loadStudentLedger, loadEnrollments, loadPromotions, loadFunding]);

  /* ---------------- Handlers ---------------- */

  const onClassChange = useCallback((e: ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value;
    const classValue = value ? Number(value) : "";
    setSelectedClass(classValue);
    setCurrentPage(0);
    setStudents([]);
    setTotalPages(0);
    setSelectedIds(new Set());
    if (classValue) {
      void loadStudents(Number(classValue), 0);
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
      const next = new Set(prev);
      const everySelected = filteredStudents.length > 0 && filteredStudents.every((s) => next.has(s.id));
      if (everySelected) {
        filteredStudents.forEach((s) => next.delete(s.id));
      } else {
        filteredStudents.forEach((s) => next.add(s.id));
      }
      return next;
    });
  }, [filteredStudents]);

  const openProfile = useCallback(async (summary: Student) => {
    try {
      setLoadingProfile(true);
      setProfileTab("overview");
      setLedgerData([]);
      setEnrollmentsData([]);
      setPromotionsData([]);
      setFundingData(null);
      setShowProfileModal(true);

      // Fetch full student details
      const [res, gRes] = await Promise.all([
        studentApi.getById(summary.id),
        studentApi.getGuardians(summary.id)
      ]);

      setProfileStudent({
        ...res.data,
        guardians: (gRes.data || []).map((g: Partial<GuardianFormValue>) => ({
          name: g.name || "",
          relation: g.relation || "FATHER",
          contactNumber: g.contactNumber || "",
          email: g.email || "",
          address: g.address || "",
          aadharNumber: g.aadharNumber || "",
          occupation: g.occupation || "",
          qualification: g.qualification || "",
          primaryGuardian: !!g.primaryGuardian,
          whatsappEnabled: g.whatsappEnabled ?? true,
        }))
      });
    } catch {
      showToast("Failed to load student profile", "error");
      setProfileStudent(summary); // Fallback to list summary
    } finally {
      setLoadingProfile(false);
    }
  }, [showToast]);

  const closeProfile = useCallback(() => {
    setShowProfileModal(false);
    setProfileStudent(null);
    setLedgerData([]);
    setEnrollmentsData([]);
    setPromotionsData([]);
    setFundingData(null);
    setFundingHistory([]);
    setProfileTab("overview");
  }, []);

  const onPromotionSuccess = useCallback(() => {
    setShowPromotionModal(false);
    setSelectedIds(new Set());
    if (selectedClass) {
      void loadStudents(Number(selectedClass), currentPage);
    }
  }, [selectedClass, currentPage, loadStudents]);

  function goToPage(nextPage: number) {
    if (!selectedClass || nextPage < 0 || nextPage >= totalPages || nextPage === currentPage) return;
    setCurrentPage(nextPage);
    void loadStudents(Number(selectedClass), nextPage);
  }

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

      const { classId, ...formData } = studentForm;

      if (isEditing && profileStudent) {
        const { guardians, ...studentUpdateData } = formData;
        await studentApi.update(profileStudent.id, {
          ...studentUpdateData,
          previousYearOfPassing: studentUpdateData.previousYearOfPassing ? Number(studentUpdateData.previousYearOfPassing) : null,
        });
        await studentApi.updateGuardians(profileStudent.id, studentForm.guardians);
        showToast("Student updated successfully!", "success");
      } else {
        const res = await studentApi.create({
          ...formData,
          previousYearOfPassing: formData.previousYearOfPassing ? Number(formData.previousYearOfPassing) : null,
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
          // Validate funding before saving
          const value = Number(studentForm.fundingValue);

          if (studentForm.fundingType === "PARTIAL") {
            if (isNaN(value) || value <= 0) {
              showToast("Funding value must be greater than 0", "error");
              setIsSaving(false);
              return;
            }
            if (studentForm.fundingMode === "PERCENTAGE" && value > 100) {
              showToast("Percentage coverage cannot exceed 100%", "error");
              setIsSaving(false);
              return;
            }
          }

          if (studentForm.fundingValidFrom && !studentForm.fundingValidTo) {
            showToast("Valid To date is required if Valid From is set", "error");
            setIsSaving(false);
            return;
          }

          if (studentForm.fundingValidFrom && studentForm.fundingValidTo) {
            if (new Date(studentForm.fundingValidFrom) >= new Date(studentForm.fundingValidTo)) {
              showToast("Valid From date must be before Valid To date", "error");
              setIsSaving(false);
              return;
            }
          }

          await api.post("/api/fees/funding", {
            studentId,
            sessionId: currentSession.id,
            coverageType: studentForm.fundingType,
            coverageMode: studentForm.fundingMode,
            coverageValue: value,
            validFrom: studentForm.fundingValidFrom || null,
            validTo: studentForm.fundingValidTo || null,
          });
        }

        showToast("Student enrolled successfully!", "success");
      }
      setShowAddModal(false);
      setIsEditing(false);

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
        void loadStudents(Number(selectedClass), currentPage);
      }
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const msg = getErrorMessage(err);
        showToast(msg, "error");
      } else {
        showToast("Unknown error", "error");
      }
    } finally {
      setIsSaving(false);
    }
  }

  async function saveFundingEdit() {
    if (!profileStudent || !currentSession) return;

    if (fundingForm.coverageType !== "NONE") {
      const value = Number(fundingForm.coverageValue);
      if (fundingForm.coverageType === "PARTIAL") {
        if (isNaN(value) || value <= 0) {
          showToast("Funding value must be greater than 0", "error");
          return;
        }
        if (fundingForm.coverageMode === "PERCENTAGE" && value > 100) {
          showToast("Percentage coverage cannot exceed 100%", "error");
          return;
        }
      }
      if (fundingForm.validFrom && !fundingForm.validTo) {
        showToast("Valid To date is required if Valid From is set", "error");
        return;
      }
      if (fundingForm.validFrom && fundingForm.validTo) {
        if (new Date(fundingForm.validFrom) >= new Date(fundingForm.validTo)) {
          showToast("Valid From date must be before Valid To date", "error");
          return;
        }
      }
    }

    try {
      setIsSaving(true);

      if (fundingData && fundingData.id) {
        await api.delete(`/api/fees/funding/${fundingData.id}`);
      }

      if (fundingForm.coverageType !== "NONE") {
        await api.post("/api/fees/funding", {
          studentId: profileStudent.id,
          sessionId: currentSession.id,
          coverageType: fundingForm.coverageType,
          coverageMode: fundingForm.coverageMode,
          coverageValue: Number(fundingForm.coverageValue),
          validFrom: fundingForm.validFrom || null,
          validTo: fundingForm.validTo || null,
        });
      }

      showToast("Funding arrangement updated successfully", "success");
      setEditingFunding(false);
      void loadFunding(profileStudent.id, currentSession.id);

    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        showToast(getErrorMessage(err), "error");
      } else {
        showToast("Failed to update funding arrangement", "error");
      }
    } finally {
      setIsSaving(false);
    }
  }

  /* ---------------- UI ---------------- */

  return (
    <div className="mx-auto px-6 py-6 space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-lg font-semibold">Student Directory</h1>
          <p className="text-gray-500 text-base mt-1">Manage students for <span className="text-blue-600 font-bold">{currentSession?.name || "current session"}</span>.</p>
        </div>

        {canUserAddStudent && (
          <div className="flex gap-2">
            {canUserPromoteStudents && (
              <button
                onClick={() => setShowPromotionModal(true)}
                disabled={selectedStudents.length === 0}
                className="bg-white border border-gray-300 px-6 py-2.5 rounded-md font-medium hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed text-base"
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
              className="bg-blue-600 text-white px-6 py-2.5 rounded-md font-medium hover:bg-blue-700 flex items-center gap-2 text-base"
            >
              <span className="text-xl">+</span> Add Student
            </button>
          </div>
        )}
      </div>

      <div className="bg-white rounded-lg shadow border border-gray-100 p-6 flex flex-wrap gap-4 items-end">
        <div className="flex-1 min-w-[300px]">
          <label className="block text-sm font-medium text-gray-500 mb-2">Filter by Class</label>
          <select
            value={selectedClass}
            onChange={onClassChange}
            disabled={loadingClasses}
            className="w-full rounded-md border border-gray-300 focus:ring-2 focus:ring-blue-500 px-3 py-2 text-base"
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
        <div className="bg-white rounded-lg shadow border border-gray-100 p-6">
          <TableSkeleton rows={10} cols={4} />
        </div>
      ) : selectedClass ? (
        <div className="bg-white rounded-lg shadow border border-gray-100 overflow-hidden mt-4">
          <div className="px-6 py-4 border-b border-gray-100 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div className="flex items-center gap-3">
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search by student name"
                className="w-64 rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
              />
              <button
                onClick={() => setSortOrder((prev) => (prev === "asc" ? "desc" : "asc"))}
                className="px-3 py-2 text-sm border border-gray-300 rounded-md bg-white hover:bg-gray-50"
              >
                Sort: {sortOrder === "asc" ? "Asc" : "Desc"}
              </button>
            </div>
            <span className="text-gray-500 text-sm">
              Showing {filteredStudents.length} students
            </span>
          </div>
          <table className="w-full text-base">
            <thead className="bg-gray-50 text-gray-600 text-lg font-semibold border-b border-gray-100">
              <tr>
                <th className="px-6 py-4 text-center w-12">
                  <input
                    type="checkbox"
                    checked={allFilteredSelected}
                    onChange={toggleSelectAll}
                  />
                </th>
                <th className="px-6 py-4 text-center w-16">Sr</th>
                <th className="px-6 py-4 text-left">Student Name</th>
                <th className="px-6 py-4 text-center">Admission No</th>
                <th className="px-6 py-4 text-center">Gender</th>
                <th className="px-6 py-4 text-center">Status</th>
                <th className="px-6 py-4 text-center">Contact</th>
                <th className="px-6 py-4 text-center w-24">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredStudents.map((s, index) => (
                <tr key={s.id} className="hover:bg-gray-50/50 transition-colors">
                  <td className="p-4 text-center">
                    <input
                      type="checkbox"
                      checked={selectedIds.has(s.id)}
                      onChange={() => toggleStudentSelection(s.id)}
                    />
                  </td>
                  <td className="p-4 text-center text-gray-500">{index + 1}</td>
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
                  <td className="p-4 text-center">
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase border ${s.currentStatus === "ENROLLED" ? "bg-green-50 text-green-600 border-green-100" :
                      s.currentStatus === "FAILED" ? "bg-orange-50 text-orange-600 border-orange-100" :
                        s.currentStatus === "LEFT" ? "bg-red-50 text-red-600 border-red-100" :
                          "bg-gray-50 text-gray-600 border-gray-100"
                      }`}>
                      {s.currentStatus || "UNKNOWN"}
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
              {filteredStudents.length === 0 && (
                <tr>
                  <td colSpan={7} className="p-20 text-center text-gray-400 italic bg-gray-50/30">
                    <div className="flex flex-col items-center gap-3">
                      <span className="text-4xl">📂</span>
                      <div>
                        <p className="font-bold text-gray-800">{students.length === 0 ? "Class is Empty" : "No students match your search"}</p>
                        <p className="text-sm">
                          {students.length === 0
                            ? "No students have been enrolled in this class for the current session."
                            : "Try a different name or clear search to see all students."}
                        </p>
                      </div>
                      {canUserAddStudent && students.length === 0 && (
                        <button
                          onClick={() => {
                            if (selectedClass) {
                              setStudentForm(prev => ({ ...prev, classId: selectedClass.toString() }));
                            }
                            setShowAddModal(true);
                          }}
                          className="mt-2 bg-blue-600 text-white px-6 py-2 rounded-md font-medium hover:bg-blue-700"
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
          {totalPages > 1 && (
            <div className="px-6 py-3 border-t border-gray-100 flex items-center justify-between">
              <button
                onClick={() => goToPage(currentPage - 1)}
                disabled={currentPage === 0}
                className="px-3 py-1.5 text-sm border border-gray-300 rounded-md bg-white hover:bg-gray-50 disabled:opacity-50"
              >
                Previous
              </button>
              <span className="text-sm text-gray-600">
                Page {currentPage + 1} of {totalPages}
              </span>
              <button
                onClick={() => goToPage(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
                className="px-3 py-1.5 text-sm border border-gray-300 rounded-md bg-white hover:bg-gray-50 disabled:opacity-50"
              >
                Next
              </button>
            </div>
          )}
        </div>
      ) : (
        <div className="p-20 text-center bg-white rounded-lg shadow border border-gray-100 text-gray-500 mb-6">
          Please select a class to view and manage students.
        </div>
      )}

      <Modal
        isOpen={showAddModal}
        onClose={() => { setShowAddModal(false); setIsEditing(false); }}
        title={isEditing ? "Edit Student Details" : "Enroll New Student"}
        maxWidth="max-w-4xl"
        bodyClassName="px-6 py-4 overflow-y-auto flex-1"
        footer={
          <div className="flex gap-2">
            <button
              onClick={() => { setShowAddModal(false); setIsEditing(false); }}
              className="px-6 py-2 rounded-md bg-white border border-gray-300 font-medium text-gray-600 hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              onClick={saveStudent}
              disabled={isSaving}
              className="px-8 py-2 rounded-md bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50"
            >
              {isSaving ? "Saving..." : (isEditing ? "Update Student" : "Enroll Student")}
            </button>
          </div>
        }
      >
        <div className="flex flex-col gap-8 custom-scrollbar">

          {/* Section 1: Admission & Enrollment */}
          <section className="space-y-4 mb-6">
            <h3 className="text-lg font-semibold border-b border-gray-100 pb-2 flex items-center gap-2">
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
          <section className="space-y-4 mb-6">
            <h3 className="text-lg font-semibold border-b border-gray-100 pb-2 flex items-center gap-2">
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
          <section className="space-y-4 mb-6">
            <h3 className="text-lg font-semibold border-b border-gray-100 pb-2 flex items-center gap-2">
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
          <section className="space-y-4 mb-6">
            <h3 className="text-lg font-semibold border-b border-gray-100 pb-2 flex items-center gap-2">
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
          <section className="space-y-4 mb-6">
            <h3 className="text-lg font-semibold border-b border-gray-100 pb-2 flex items-center gap-2">
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
          {canUserAddStudent && (
            <section className="space-y-4 bg-gray-50 p-4 rounded-lg border border-gray-100 mb-6">
              <h3 className="text-lg font-semibold border-b border-gray-100 pb-2 flex items-center gap-2">
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

          {/* Section 7: Remarks */}
          <section className="space-y-4 mb-6">
            <h3 className="text-lg font-semibold border-b border-gray-100 pb-2 flex items-center gap-2">
              <span className="bg-blue-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-[10px]">7</span>
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

          {/* Section 8: Guardians */}
          <section className="space-y-4 mb-4">
            <h3 className="text-lg font-semibold border-b border-gray-100 pb-2 flex items-center gap-2">
              <span className="bg-blue-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-[10px]">8</span>
              Guardian Information
            </h3>
            <GuardianFormSection
              guardians={studentForm.guardians}
              onChange={(newGuardians) => setStudentForm({ ...studentForm, guardians: newGuardians })}
            />
          </section>

        </div>
      </Modal>

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
              className={`px-3 py-1.5 rounded-md text-base font-medium ${profileTab === "overview" ? "bg-blue-600 text-white hover:bg-blue-700" : "bg-white border border-gray-300 text-gray-700 hover:bg-gray-50"}`}
            >
              Overview
            </button>
            <button
              onClick={() => setProfileTab("ledger")}
              className={`px-3 py-1.5 rounded-md text-base font-medium ${profileTab === "ledger" ? "bg-blue-600 text-white hover:bg-blue-700" : "bg-white border border-gray-300 text-gray-700 hover:bg-gray-50"}`}
            >
              Financial Ledger
            </button>
            <button
              onClick={() => setProfileTab("enrollments")}
              className={`px-3 py-1.5 rounded-md text-base font-medium ${profileTab === "enrollments" ? "bg-blue-600 text-white hover:bg-blue-700" : "bg-white border border-gray-300 text-gray-700 hover:bg-gray-50"}`}
            >
              Enrollments
            </button>
            <button
              onClick={() => setProfileTab("funding")}
              className={`px-3 py-1.5 rounded-md text-base font-medium ${profileTab === "funding" ? "bg-blue-600 text-white hover:bg-blue-700" : "bg-white border border-gray-300 text-gray-700 hover:bg-gray-50"}`}
            >
              Funding
            </button>
          </div>

          {profileTab === "overview" && (
            loadingProfile ? (
              <div className="py-10 text-center text-gray-400 italic">Loading details...</div>
            ) : profileStudent ? (
              <div className="space-y-6">
                <div className="grid grid-cols-2 lg:grid-cols-3 gap-6 text-sm">
                  <div className="space-y-1">
                    <span className="text-[10px] font-bold text-gray-400 uppercase block tracking-wider">Admission No</span>
                    <span className="text-gray-900 font-medium">{profileStudent.admissionNumber || "-"}</span>
                  </div>
                  <div className="space-y-1">
                    <span className="text-[10px] font-bold text-gray-400 uppercase block tracking-wider">Admission Date</span>
                    <span className="text-gray-900 font-medium">{profileStudent.dateOfAdmission || "-"}</span>
                  </div>
                  <div className="space-y-1">
                    <span className="text-[10px] font-bold text-gray-400 uppercase block tracking-wider">Gender</span>
                    <span className="text-gray-900 font-medium uppercase">{profileStudent.gender || "-"}</span>
                  </div>
                  <div className="space-y-1">
                    <span className="text-[10px] font-bold text-gray-400 uppercase block tracking-wider">Date of Birth</span>
                    <span className="text-gray-900 font-medium">{profileStudent.dob || "-"}</span>
                  </div>
                  <div className="space-y-1">
                    <span className="text-[10px] font-bold text-gray-400 uppercase block tracking-wider">Contact</span>
                    <span className="text-gray-900 font-medium">{profileStudent.contactNumber || "-"}</span>
                  </div>
                  <div className="space-y-1">
                    <span className="text-[10px] font-bold text-gray-400 uppercase block tracking-wider">Email</span>
                    <span className="text-gray-900 font-medium">{profileStudent.email || "-"}</span>
                  </div>
                </div>

                <div className="bg-gray-50/50 p-4 rounded-2xl border space-y-4">
                  <h4 className="text-[10px] font-bold text-blue-600 uppercase tracking-widest border-b pb-2">Identity & Demographics</h4>
                  <div className="grid grid-cols-2 lg:grid-cols-3 gap-4 text-xs">
                    <div><span className="text-gray-400 block mb-0.5">Aadhar Number:</span> <span className="font-mono">{profileStudent.aadharNumber || "-"}</span></div>
                    <div><span className="text-gray-400 block mb-0.5">PEN:</span> <span className="font-mono">{profileStudent.pen || "-"}</span></div>
                    <div><span className="text-gray-400 block mb-0.5">Blood Group:</span> {profileStudent.bloodGroup || "-"}</div>
                    <div><span className="text-gray-400 block mb-0.5">Religion:</span> {profileStudent.religion || "-"}</div>
                    <div><span className="text-gray-400 block mb-0.5">Category:</span> {profileStudent.category || "-"}</div>
                    <div><span className="text-gray-400 block mb-0.5">Caste:</span> {profileStudent.caste || "-"}</div>
                  </div>
                </div>

                <div className="space-y-2">
                  <span className="text-[10px] font-bold text-gray-400 uppercase block tracking-wider">Address</span>
                  <p className="text-gray-700 leading-relaxed text-xs">
                    {[profileStudent.address, profileStudent.city, profileStudent.state, profileStudent.pincode].filter(Boolean).join(", ")}
                    {!profileStudent.address && "-"}
                  </p>
                </div>

                {profileStudent.guardians && profileStudent.guardians.length > 0 && (
                  <div className="space-y-3">
                    <h4 className="text-[10px] font-bold text-gray-400 uppercase tracking-widest border-b pb-2">Guardians</h4>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      {profileStudent.guardians.map((g, idx) => (
                        <div key={idx} className={`p-3 rounded-xl border flex justify-between items-center ${g.primaryGuardian ? 'bg-blue-50/50 border-blue-100' : 'bg-white border-gray-100'}`}>
                          <div>
                            <p className="text-xs font-bold text-gray-800">{g.name} <span className="text-[9px] text-gray-400 font-normal">({g.relation})</span></p>
                            <p className="text-[10px] text-gray-500">{g.contactNumber}</p>
                          </div>
                          {g.primaryGuardian && <span className="text-[8px] bg-blue-600 text-white px-1.5 py-0.5 rounded-full font-bold uppercase">Primary</span>}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {canUserEditStudent && (
                  <div className="flex justify-end pt-4 border-t">
                    <button
                      onClick={() => {
                        setIsEditing(true);
                        setStudentForm({
                          admissionNumber: profileStudent.admissionNumber || "",
                          firstName: profileStudent.firstName || "",
                          lastName: profileStudent.lastName || "",
                          dob: profileStudent.dob || "",
                          gender: profileStudent.gender || "",
                          pen: profileStudent.pen || "",
                          aadharNumber: profileStudent.aadharNumber || "",
                          religion: profileStudent.religion || "",
                          caste: profileStudent.caste || "",
                          category: profileStudent.category || "",
                          address: profileStudent.address || "",
                          city: profileStudent.city || "",
                          state: profileStudent.state || "",
                          pincode: profileStudent.pincode || "",
                          contactNumber: profileStudent.contactNumber || "",
                          email: profileStudent.email || "",
                          bloodGroup: profileStudent.bloodGroup || "",
                          dateOfAdmission: profileStudent.dateOfAdmission || new Date().toISOString().split('T')[0],
                          remarks: profileStudent.remarks || "",
                          previousSchoolName: profileStudent.previousSchoolName || "",
                          previousSchoolBoard: profileStudent.previousSchoolBoard || "",
                          previousClass: profileStudent.previousClass || "",
                          previousYearOfPassing: profileStudent.previousYearOfPassing?.toString() || "",
                          transferCertificateNumber: profileStudent.transferCertificateNumber || "",
                          previousSchoolAddress: profileStudent.previousSchoolAddress || "",
                          previousSchoolContact: profileStudent.previousSchoolContact || "",
                          reasonForLeavingPreviousSchool: profileStudent.reasonForLeavingPreviousSchool || "",
                          classId: selectedClass?.toString() || "",
                          guardians: profileStudent.guardians || [],
                          fundingType: "NONE", // Funding editing might need more logic
                          fundingMode: "FIXED_AMOUNT",
                          fundingValue: "0",
                          fundingValidFrom: "",
                          fundingValidTo: "",
                        });
                        setShowProfileModal(false);
                        setShowAddModal(true);
                      }}
                      className="bg-blue-600 text-white px-6 py-2 rounded-md font-medium hover:bg-blue-700 flex items-center gap-2"
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.172-2.172a2.828 2.828 0 114 4L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                      Edit Student Details
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="py-20 text-center text-gray-400 italic bg-gray-50 rounded-2xl border border-dashed">
                Student data could not be loaded.
              </div>
            )
          )}

          {profileTab === "ledger" && (
            <div className="space-y-3">
              {ledgerLoading ? (
                <TableSkeleton rows={3} cols={4} />
              ) : ledgerData.length === 0 ? (
                <div className="text-sm text-gray-500">No ledger entries found.</div>
              ) : (
                ledgerData.map((entry) => (
                  <details
                    key={`${entry.sessionId}-${entry.sessionName}`}
                    className="border rounded-xl p-3"
                    open={entry.active === true}
                  >
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

          {profileTab === "enrollments" && (
            <div className="space-y-3">
              {enrollmentsLoading ? (
                <TableSkeleton rows={3} cols={5} />
              ) : enrollmentsData.length === 0 ? (
                <div className="text-sm text-gray-500">No enrollment history found.</div>
              ) : (
                <div className="overflow-hidden border border-gray-200 rounded-lg">
                  <table className="min-w-full divide-y divide-gray-200 text-sm">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-[10px]">Session</th>
                        <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-[10px]">Class</th>
                        <th className="px-4 py-3 text-center font-medium text-gray-500 uppercase tracking-wider text-[10px]">Roll No</th>
                        <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-[10px]">Date Enrolled</th>
                        <th className="px-4 py-3 text-center font-medium text-gray-500 uppercase tracking-wider text-[10px]">Status</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {enrollmentsData.map((e) => (
                        <tr key={e.id} className={e.active ? "bg-green-50/20" : ""}>
                          <td className="px-4 py-3 font-medium text-gray-900">
                            {sessions.find((s: any) => s.id === e.sessionId)?.name || `Session ${e.sessionId}`}
                          </td>
                          <td className="px-4 py-3 text-gray-700">
                            {classes.find(c => c.id === e.classId)?.name || `Class ${e.classId}`} {e.section ? `- ${e.section}` : ''}
                          </td>
                          <td className="px-4 py-3 text-center font-mono text-gray-600">
                            {e.rollNumber || "-"}
                          </td>
                          <td className="px-4 py-3 text-gray-500">
                            {e.enrollmentDate || "-"}
                          </td>
                          <td className="px-4 py-3 text-center">
                            <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase border ${e.active ? 'bg-green-50 text-green-700 border-green-200' : 'bg-gray-100 text-gray-600 border-gray-200'}`}>
                              {e.active ? "Active" : "Past"}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {profileTab === "promotions" && (
            <div className="space-y-3">
              {promotionsLoading ? (
                <TableSkeleton rows={3} cols={6} />
              ) : promotionsData.length === 0 ? (
                <div className="text-sm text-gray-500">No promotion history found.</div>
              ) : (
                <div className="overflow-hidden border border-gray-200 rounded-lg">
                  <table className="min-w-full divide-y divide-gray-200 text-sm">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-[10px]">Type</th>
                        <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-[10px]">From Session</th>
                        <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-[10px]">To Session</th>
                        <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-[10px]">From Class</th>
                        <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider text-[10px]">To Class</th>
                        <th className="px-4 py-3 text-center font-medium text-gray-500 uppercase tracking-wider text-[10px]">Date / By</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {promotionsData.map((p) => (
                        <tr key={p.id}>
                          <td className="px-4 py-3 text-[11px] font-bold">
                            <span className={`px-2 py-0.5 rounded-full uppercase tracking-wider ${p.promotionType === 'PROMOTED' || p.promotionType === 'GRADUATED'
                              ? 'bg-green-100 text-green-700'
                              : 'bg-red-100 text-red-700'
                              }`}>
                              {p.promotionType}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-gray-800 text-[11px]">
                            {p.sourceSessionName || `Session ${p.sourceSessionId}`}
                          </td>
                          <td className="px-4 py-3 font-semibold text-blue-800 text-[11px]">
                            {p.targetSessionName || `Session ${p.targetSessionId}`}
                          </td>
                          <td className="px-4 py-3 text-gray-800 text-[11px]">
                            {p.sourceClassName || `Class ${p.sourceClassId}`}
                          </td>
                          <td className="px-4 py-3 font-semibold text-blue-800 text-[11px]">
                            {p.targetClassName || `Class ${p.targetClassId}`}
                          </td>
                          <td className="px-4 py-3 text-center">
                            <div className="text-[10px] text-gray-900 font-mono">
                              {new Date(p.promotedAt).toLocaleDateString()}
                            </div>
                            <div className="text-[9px] text-gray-400 mt-1 uppercase">
                              by {p.promotedBy}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {profileTab === "funding" && (
            <div className="space-y-4">
              {fundingLoading ? (
                <div className="py-10 text-center text-gray-400 italic">Loading funding details...</div>
              ) : editingFunding ? (
                <div className="bg-gray-50 p-6 rounded-xl border space-y-4">
                  <h3 className="text-sm font-semibold border-b pb-2">Edit Funding Arrangement</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-1">
                      <label className="text-[10px] font-bold text-gray-500 uppercase tracking-wider">Coverage Type</label>
                      <select
                        value={fundingForm.coverageType}
                        onChange={(e) => setFundingForm({ ...fundingForm, coverageType: e.target.value as any })}
                        className="input-ref"
                      >
                        <option value="NONE">None / Self-Paid</option>
                        <option value="FULL">Full Scholarship (100%)</option>
                        <option value="PARTIAL">Partial Sponsorship</option>
                      </select>
                    </div>

                    {fundingForm.coverageType === "PARTIAL" && (
                      <>
                        <div className="space-y-1">
                          <label className="text-[10px] font-bold text-gray-500 uppercase tracking-wider">Mode</label>
                          <select
                            value={fundingForm.coverageMode}
                            onChange={(e) => setFundingForm({ ...fundingForm, coverageMode: e.target.value as any })}
                            className="input-ref"
                          >
                            <option value="FIXED_AMOUNT">Fixed Amount ($)</option>
                            <option value="PERCENTAGE">Percentage (%)</option>
                          </select>
                        </div>
                        <div className="space-y-1">
                          <label className="text-[10px] font-bold text-gray-500 uppercase tracking-wider">Value</label>
                          <input
                            type="number"
                            placeholder="0.00"
                            value={fundingForm.coverageValue}
                            onChange={(e) => setFundingForm({ ...fundingForm, coverageValue: e.target.value })}
                            className="input-ref"
                          />
                        </div>
                      </>
                    )}

                    {fundingForm.coverageType !== "NONE" && (
                      <>
                        <div className="space-y-1">
                          <label className="text-[10px] font-bold text-gray-500 uppercase tracking-wider">Valid From</label>
                          <input
                            type="date"
                            value={fundingForm.validFrom}
                            onChange={(e) => setFundingForm({ ...fundingForm, validFrom: e.target.value })}
                            className="input-ref"
                          />
                        </div>
                        <div className="space-y-1">
                          <label className="text-[10px] font-bold text-gray-500 uppercase tracking-wider">Valid To</label>
                          <input
                            type="date"
                            value={fundingForm.validTo}
                            onChange={(e) => setFundingForm({ ...fundingForm, validTo: e.target.value })}
                            className="input-ref"
                          />
                        </div>
                      </>
                    )}
                  </div>

                  <div className="flex justify-end gap-2 pt-4 border-t mt-4">
                    <button
                      onClick={() => {
                        setEditingFunding(false);
                        setFundingForm({
                          coverageType: fundingData?.coverageType || "NONE",
                          coverageMode: fundingData?.coverageMode || "FIXED_AMOUNT",
                          coverageValue: fundingData?.coverageValue ? fundingData.coverageValue.toString() : "0",
                          validFrom: fundingData?.validFrom || "",
                          validTo: fundingData?.validTo || "",
                        });
                      }}
                      className="px-4 py-2 rounded-md bg-white border border-gray-300 text-sm font-medium hover:bg-gray-50"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={saveFundingEdit}
                      disabled={isSaving}
                      className="px-4 py-2 rounded-md bg-blue-600 text-white text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                    >
                      {isSaving ? "Saving..." : "Save Changes"}
                    </button>
                  </div>
                </div>
              ) : fundingData && fundingData.coverageType !== "NONE" ? (
                <div className="bg-white p-6 rounded-xl border shadow-sm flex flex-col gap-6 relative">
                  {canUserEditStudent && (
                    <button
                      onClick={() => setEditingFunding(true)}
                      className="absolute top-4 right-4 text-blue-600 hover:text-blue-800 text-sm font-medium"
                    >
                      Edit
                    </button>
                  )}
                  <div className="flex items-center gap-3 border-b border-gray-100 pb-4">
                    <span className={`w-10 h-10 rounded-full flex items-center justify-center text-xl ${fundingData.coverageType === 'FULL' ? 'bg-green-100 text-green-600' : 'bg-blue-100 text-blue-600'}`}>
                      💎
                    </span>
                    <div>
                      <h3 className="text-lg font-bold text-gray-800">
                        {fundingData.coverageType === "FULL" ? "Full Scholarship" : "Partial Sponsorship"}
                      </h3>
                      <p className="text-sm text-gray-500 flex items-center gap-2">
                        <span className="w-2 h-2 rounded-full bg-green-500"></span>
                        Active Arrangement
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                    <div className="space-y-1">
                      <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">Coverage Value</span>
                      <span className="text-xl font-bold font-mono text-gray-800">
                        {fundingData.coverageType === "FULL"
                          ? "100%"
                          : fundingData.coverageMode === "PERCENTAGE"
                            ? `${fundingData.coverageValue}%`
                            : `₹${Number(fundingData.coverageValue).toLocaleString()}`
                        }
                      </span>
                    </div>
                    <div className="space-y-1">
                      <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">Calculation Mode</span>
                      <span className="text-sm font-medium text-gray-700 bg-gray-100 px-2 py-1 rounded">
                        {fundingData.coverageMode === "PERCENTAGE" ? "Percentage Based" : "Fixed Amount Deduction"}
                      </span>
                    </div>
                    <div className="space-y-1">
                      <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">Valid From</span>
                      <span className="text-sm font-medium text-gray-700">{fundingData.validFrom || "No Start Date"}</span>
                    </div>
                    <div className="space-y-1">
                      <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">Valid To</span>
                      <span className="text-sm font-medium text-gray-700">{fundingData.validTo || "No Expiry"}</span>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="py-12 bg-gray-50 border border-dashed border-gray-300 rounded-xl text-center flex flex-col items-center gap-3">
                  <span className="text-3xl">💰</span>
                  <div>
                    <p className="text-gray-800 font-medium">No Funding Arrangement</p>
                    <p className="text-gray-500 text-sm">This student pays full fees natively.</p>
                  </div>
                  {canUserEditStudent && (
                    <button
                      onClick={() => setEditingFunding(true)}
                      className="mt-2 text-blue-600 bg-blue-50 hover:bg-blue-100 px-4 py-2 rounded-md font-medium text-sm transition-colors border border-blue-200"
                    >
                      {fundingHistory.length > 0 ? "+ Re-add Funding" : "+ Add Sponsorship"}
                    </button>
                  )}
                </div>
              )}

              {fundingHistory.length > 0 && (
                <div className="mt-6 border-t pt-4">
                  <h4 className="text-sm font-semibold mb-3">Funding History</h4>
                  <div className="space-y-2">
                    {fundingHistory.map((hist) => (
                      <div key={hist.id} className="flex justify-between items-center p-3 bg-white border border-gray-100 rounded-lg text-sm">
                        <div>
                          <span className="font-semibold text-gray-700">{hist.coverageType === "FULL" ? "Full Scholarship" : "Partial Sponsorship"}</span>
                          <span className="text-xs text-gray-500 ml-2">({hist.coverageMode === "PERCENTAGE" ? hist.coverageValue + "%" : "₹" + hist.coverageValue})</span>
                        </div>
                        <span className={`px-2 py-0.5 rounded-full text-[10px] uppercase font-bold ${hist.active ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-500"}`}>
                          {hist.active ? "Active" : "Inactive"}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </Modal>
    </div >
  );
}
