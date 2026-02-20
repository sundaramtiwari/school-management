"use client";

import { ChangeEvent, useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { schoolApi } from "@/lib/schoolApi";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/components/ui/Toast";
import Modal from "@/components/ui/Modal";
import { TableSkeleton } from "@/components/ui/Skeleton";

/* ---------------- Types ---------------- */

type School = {
  id: number;
  name: string;
  displayName: string;
  board: string;
  medium: string;
  schoolCode: string;
  affiliationCode?: string;
  city: string;
  state: string;
  contactEmail: string;
  contactNumber: string;
  address: string;
  pincode: string;
  website: string;
  description: string;
};

/* ---------------- Regex ---------------- */

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
/* ---------------- Page ---------------- */

export default function SchoolsPage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const router = useRouter();

  /* -------- UI State -------- */

  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    const currentRole = user?.role?.toUpperCase();
    if (user && currentRole !== "SUPER_ADMIN" && currentRole !== "PLATFORM_ADMIN") {
      router.push("/");
    }
  }, [router, user]);

  /* -------- Data State -------- */

  const [schools, setSchools] = useState<School[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  /* -------- Form State -------- */

  const [form, setForm] = useState({
    name: "",
    displayName: "",
    board: "",
    medium: "",
    schoolCode: "",
    city: "",
    state: "",
    contactEmail: "",
    contactNumber: "",
    address: "",
    pincode: "",
    website: "",
    description: "",
    adminName: "",
    adminEmail: "",
    adminPassword: "",
  });

  /* ---------------- Load ---------------- */

  const loadSchools = useCallback(async () => {
    try {
      setLoading(true);
      const res = await schoolApi.list();
      setSchools(res.data.content || []);
    } catch {
      setError("Failed to load schools");
      showToast("Error loading schools", "error");
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    loadSchools();
  }, [loadSchools]);

  /* ---------------- Helpers ---------------- */

  function updateField(e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    setForm({
      ...form,
      [e.target.name]: e.target.value,
    });
  }

  function getErrorMessage(error: unknown): string {
    if (error && typeof error === "object" && "response" in error) {
      const response = (error as { response?: { data?: { message?: string } } }).response;
      if (response?.data?.message) return response.data.message;
    }
    if (error instanceof Error) return error.message;
    return "Unknown error";
  }

  function resetForm() {
    setForm({
      name: "",
      displayName: "",
      board: "",
      medium: "",
      schoolCode: "",
      city: "",
      state: "",
      contactEmail: "",
      contactNumber: "",
      address: "",
      pincode: "",
      website: "",
      description: "",
      adminName: "",
      adminEmail: "",
      adminPassword: "",
    });
  }

  /* ---------------- Validation ---------------- */

  function validate() {
    if (!form.name) return "Name required";
    if (!form.board) return "Board required";
    if (!form.medium) return "Medium required";
    if (!form.city) return "City required";
    if (!form.state) return "State required";
    if (!EMAIL_REGEX.test(form.contactEmail)) return "Invalid school email";

    if (!editId) {
      if (!form.adminEmail || !EMAIL_REGEX.test(form.adminEmail)) return "Valid Admin Email required";
      if (!form.adminPassword || form.adminPassword.length < 6) return "Admin Password required (min 6 chars)";
      if (!form.adminName) return "Admin Name required";
    }

    return null;
  }

  /* ---------------- Save ---------------- */

  async function saveSchool() {
    const err = validate();
    if (err) {
      showToast(err, "warning");
      return;
    }

    try {
      setIsSaving(true);
      if (editId) {
        await schoolApi.update(form.schoolCode, form);
        showToast("School updated successfully!", "success");
      } else {
        await schoolApi.onboard(form);
        showToast("School onboarded successfully!", "success");
      }

      setShowForm(false);
      setEditId(null);
      resetForm();
      loadSchools();
    } catch (e: unknown) {
      showToast("Save failed: " + getErrorMessage(e), "error");
    } finally {
      setIsSaving(false);
    }
  }

  /* ---------------- Open School --------------- */
  function openSchool(s: School) {
    localStorage.setItem("schoolId", String(s.id));
    localStorage.setItem("schoolName", s.name);
    // Hard refresh to trigger context bootstrap
    router.push("/students");
  }

  /* ---------------- Edit ---------------- */

  function openEdit(s: School) {
    setForm({
      name: s.name || "",
      displayName: s.displayName || "",
      board: s.board || "",
      medium: s.medium || "",
      schoolCode: s.schoolCode || "",
      city: s.city || "",
      state: s.state || "",
      contactEmail: s.contactEmail || "",
      contactNumber: s.contactNumber || "",
      address: s.address || "",
      pincode: s.pincode || "",
      website: s.website || "",
      description: s.description || "",
      adminName: "",
      adminEmail: "",
      adminPassword: "",
    });

    setEditId(s.id);
    setShowForm(true);
  }

  /* ---------------- UI ---------------- */

  return (
    <div className="mx-auto px-6 py-6 space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-lg font-semibold">Schools</h1>
          <p className="text-gray-500 text-base mt-1">Manage school institutions and their configurations.</p>
        </div>

        {(user?.role === "SUPER_ADMIN" || user?.role === "PLATFORM_ADMIN") && (
          <button
            onClick={() => {
              resetForm();
              setEditId(null);
              setShowForm(true);
            }}
            className="bg-blue-600 text-white px-6 py-2.5 rounded-md font-medium hover:bg-blue-700 flex items-center gap-2 text-base"
          >
            <span className="text-xl">+</span> Add School
          </button>
        )}
      </div>

      {loading ? (
        <div className="bg-white rounded-lg shadow border border-gray-100 p-6">
          <TableSkeleton rows={8} cols={5} />
        </div>
      ) : error ? (
        <div className="bg-red-50 p-6 rounded-lg border border-red-100 text-red-600 font-medium">
          {error}
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow border border-gray-100 overflow-hidden mt-4">
          <table className="w-full text-base">
            <thead className="bg-gray-50 text-gray-600 text-lg font-semibold border-b border-gray-100">
              <tr>
                <th className="px-6 py-4 text-left">School Name</th>
                <th className="px-6 py-4 text-center w-24">Code</th>
                <th className="px-6 py-4 text-center">Board</th>
                <th className="px-6 py-4 text-center">Location</th>
                <th className="px-6 py-4 text-center w-32">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {schools.map((s) => (
                <tr
                  key={s.id}
                  className="hover:bg-gray-50 transition-colors group"
                >
                  <td className="p-4">
                    <div className="flex flex-col">
                      <span className="font-bold text-gray-800 group-hover:text-blue-600 transition-colors">{s.name}</span>
                      <span className="text-xs text-gray-500">{s.contactEmail}</span>
                    </div>
                  </td>
                  <td className="p-4 text-center font-mono text-gray-600 bg-gray-50/50">{s.schoolCode}</td>
                  <td className="p-4 text-center">
                    <span className="px-3 py-1 bg-blue-50 text-blue-700 rounded-full font-semibold text-xs border border-blue-100 uppercase">
                      {s.board}
                    </span>
                  </td>
                  <td className="p-4 text-center text-gray-600">{s.city}, {s.state}</td>
                  <td className="p-4 text-center">
                    <div className="flex justify-center gap-2">
                      <button
                        onClick={() => openEdit(s)}
                        className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-all"
                        title="Edit School"
                      >
                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-5M16.5 3.5a2.121 2.121 0 113 3L11.707 15.364a2 2 0 01-.88.524l-4 1a1 1 0 01-1.213-1.213l1-4a2 2 0 01.524-.88L16.5 3.5z" />
                        </svg>
                      </button>
                      <button
                        onClick={() => openSchool(s)}
                        className="p-2 text-green-600 hover:bg-green-50 rounded-lg transition-all"
                        title="View Students"
                      >
                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                        </svg>
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {schools.length === 0 && (
                <tr>
                  <td colSpan={5} className="p-12 text-center text-gray-400 italic bg-gray-50/30">
                    No schools found in the system.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Form Modal */}
      <Modal
        isOpen={showForm}
        onClose={() => setShowForm(false)}
        title={editId ? "Edit School Details" : "Onboard New School"}
        maxWidth="max-w-3xl"
        footer={
          <div className="flex gap-2">
            <button
              onClick={() => setShowForm(false)}
              className="px-6 py-2 rounded-md bg-white border border-gray-300 font-medium text-gray-600 hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              onClick={saveSchool}
              disabled={isSaving}
              className="px-8 py-2 rounded-md bg-blue-600 text-white font-medium hover:bg-blue-700 disabled:opacity-50"
            >
              {isSaving ? "Saving..." : editId ? "Update Changes" : "Create & Provision"}
            </button>
          </div>
        }
      >
        <div className="grid grid-cols-2 gap-5">
          {!editId && (
            <div className="col-span-2 grid grid-cols-2 gap-4 bg-gray-50 p-5 rounded-2xl border border-dashed border-gray-300">
              <h3 className="col-span-2 font-bold text-gray-700 flex items-center gap-2">
                <span className="w-6 h-6 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-xs">1</span>
                Initial Admin Account
              </h3>
              <div className="col-span-2 text-xs text-gray-500 mb-2">This user will be created as the first SCHOOL_ADMIN for this institution.</div>

              <input
                name="adminName"
                placeholder="Admin Full Name"
                value={form.adminName}
                onChange={updateField}
                className="input-ref"
              />
              <input
                name="adminEmail"
                type="email"
                placeholder="Admin Email Address"
                value={form.adminEmail}
                onChange={updateField}
                className="input-ref"
              />
              <input
                name="adminPassword"
                type="password"
                placeholder="Initial Password"
                value={form.adminPassword}
                onChange={updateField}
                className="input-ref col-span-2"
              />
            </div>
          )}

          <h3 className="col-span-2 font-bold text-gray-700 mt-2 flex items-center gap-2">
            <span className="w-6 h-6 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-xs">2</span>
            School Identification
          </h3>

          <input
            name="name"
            maxLength={100}
            placeholder="Institutional Name *"
            value={form.name}
            onChange={updateField}
            className="input-ref"
          />

          <input
            name="displayName"
            maxLength={100}
            placeholder="Short Display Name"
            value={form.displayName}
            onChange={updateField}
            className="input-ref"
          />

          <input
            name="board"
            maxLength={50}
            placeholder="Educational Board (e.g. CBSE) *"
            value={form.board}
            onChange={updateField}
            className="input-ref"
          />

          <input
            name="medium"
            maxLength={50}
            placeholder="Medium of Instruction *"
            value={form.medium}
            onChange={updateField}
            className="input-ref"
          />

          <h3 className="col-span-2 font-bold text-gray-700 mt-4 flex items-center gap-2">
            <span className="w-6 h-6 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-xs">3</span>
            Contact & Location
          </h3>

          <input
            name="contactEmail"
            type="email"
            maxLength={100}
            placeholder="Institutional Email *"
            value={form.contactEmail}
            onChange={updateField}
            className="input-ref"
          />

          <input
            name="contactNumber"
            maxLength={15}
            placeholder="Phone Number"
            value={form.contactNumber}
            onChange={updateField}
            className="input-ref"
          />

          <input
            name="city"
            maxLength={50}
            placeholder="City *"
            value={form.city}
            onChange={updateField}
            className="input-ref"
          />

          <input
            name="state"
            maxLength={50}
            placeholder="State *"
            value={form.state}
            onChange={updateField}
            className="input-ref"
          />

          <textarea
            name="address"
            maxLength={300}
            placeholder="Full Street Address"
            value={form.address}
            onChange={updateField}
            className="input-ref col-span-2 h-20"
          />
        </div>
      </Modal>

      <style jsx>{`
        .input-ref {
            @apply w-full border border-gray-200 rounded-xl px-4 py-2.5 focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all;
        }
      `}</style>
    </div>
  );
}
