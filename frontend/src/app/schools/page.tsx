"use client";

import { useEffect, useState } from "react";
import { schoolApi } from "@/lib/schoolApi";

/* ---------------- Types ---------------- */

type School = {
  id: number;
  name: string;
  displayName: string;
  board: string;
  medium: string;
  schoolCode: string;
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
const URL_REGEX = /^(https?:\/\/)?([\w\d-]+\.)+[\w-]{2,}(\/.*)?$/;

/* ---------------- Page ---------------- */

export default function SchoolsPage() {

  /* -------- UI State -------- */

  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);

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
    schoolCode: "", // backend generated
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

  useEffect(() => {
    loadSchools();
  }, []);

  async function loadSchools() {
    try {
      setLoading(true);
      const res = await schoolApi.list();
      setSchools(res.data.content || []);
    } catch {
      setError("Failed to load schools");
    } finally {
      setLoading(false);
    }
  }

  /* ---------------- Helpers ---------------- */

  function updateField(e: any) {
    setForm({
      ...form,
      [e.target.name]: e.target.value,
    });
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

    if (!form.name || form.name.length > 100)
      return "Name required (max 100 chars)";

    if (!form.board || form.board.length > 50)
      return "Board required (max 50 chars)";

    if (!form.medium || form.medium.length > 50)
      return "Medium required (max 50 chars)";

    if (!form.city || form.city.length > 50)
      return "City required (max 50 chars)";

    if (!form.state || form.state.length > 50)
      return "State required (max 50 chars)";

    if (!EMAIL_REGEX.test(form.contactEmail))
      return "Invalid email";

    if (form.contactNumber && !/^\d{7,15}$/.test(form.contactNumber))
      return "Phone must be 7–15 digits";

    if (form.pincode && !/^\d{4,10}$/.test(form.pincode))
      return "Invalid pincode";

    if (form.website && !URL_REGEX.test(form.website))
      return "Invalid website URL";

    if (form.address.length > 300)
      return "Address max 300 chars";

    if (form.description.length > 500)
      return "Description max 500 chars";

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
      alert(err);
      return;
    }

    try {

      if (editId) {
        await schoolApi.update(form.schoolCode, form);
      } else {
        // Create Mode - Use Onboard
        if (!form.adminEmail || !form.adminPassword) {
          alert("Admin Email and Password are required for new schools");
          return;
        }
        await schoolApi.onboard(form);
      }

      setShowForm(false);
      setEditId(null);

      resetForm();
      loadSchools();

    } catch (e: any) {
      alert("Save failed: " + (e.response?.data?.message || e.message));
    }
  }

  /* ---------------- Open Students Tab --------------- */
  function openSchool(s: any) {
    localStorage.setItem("schoolId", String(s.id));
    window.location.href = "/students";
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
    <div className="space-y-6">

      {/* Header */}

      <div className="flex justify-between items-center">

        <h1 className="text-2xl font-bold">Schools</h1>

        <button
          onClick={() => {
            resetForm();
            setEditId(null);
            setShowForm(true);
          }}
          className="bg-blue-600 text-white px-4 py-2 rounded"
        >
          + Add School
        </button>

      </div>

      {/* States */}

      {loading && <p>Loading...</p>}
      {error && <p className="text-red-500">{error}</p>}

      {/* Table */}

      {!loading && !error && (

        <div className="bg-white border rounded">

          <table className="w-full text-sm">

            <thead className="bg-gray-100">
              <tr>
                <th className="p-3 text-left">Name</th>
                <th className="p-3 text-center">Code</th>
                <th className="p-3 text-center">Board</th>
                <th className="p-3 text-center">City</th>
                <th className="p-3 text-center">Email</th>
                <th className="p-3 text-center">Actions</th>
              </tr>
            </thead>

            <tbody>

              {schools.map((s) => (

                <tr
                  key={s.id}
                  onClick={() => openEdit(s)}
                  className="border-t cursor-pointer hover:bg-gray-50"
                >
                  <td
                    className="p-3 text-blue-600 cursor-pointer hover:underline"
                    onClick={() => openSchool(s)}
                  >
                    {s.name}
                  </td>
                  <td className="p-3 text-center">{s.schoolCode}</td>
                  <td className="p-3 text-center">{s.board}</td>
                  <td className="p-3 text-center">{s.city}</td>
                  <td className="p-3 text-center">{s.contactEmail}</td>
                  <td className="p-3 text-center">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        openEdit(s);
                      }}
                      className="text-blue-600 hover:text-blue-800 text-sm font-medium"
                    >
                      Edit
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

        </div>
      )}

      {/* Modal */}

      {showForm && (

        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">

          <div className="bg-white p-6 rounded w-[700px] max-h-[90vh] overflow-auto space-y-4">

            {/* Header */}

            <div className="flex justify-between items-center">

              <h2 className="font-semibold text-lg">
                {editId ? "Edit School" : "Add School"}
              </h2>

              <button
                onClick={() => {
                  setShowForm(false);
                  setEditId(null);
                }}
                className="text-gray-500 text-xl"
              >
                ✕
              </button>

            </div>

            {/* Form */}

            {/* Form */}

            <div className="grid grid-cols-2 gap-4">

              {/* Admin Details (Only for New Schools) */}
              {!editId && (
                <div className="col-span-2 grid grid-cols-2 gap-4 border-b pb-4 mb-4">
                  <h3 className="col-span-2 font-semibold text-gray-700">Initial Admin User</h3>

                  <input
                    name="adminName"
                    placeholder="Admin Name"
                    value={form.adminName}
                    onChange={updateField}
                    className="input"
                  />
                  <input
                    name="adminEmail"
                    type="email"
                    placeholder="Admin Email *"
                    value={form.adminEmail}
                    onChange={updateField}
                    className="input"
                  />
                  <input
                    name="adminPassword"
                    type="password"
                    placeholder="Admin Password *"
                    value={form.adminPassword}
                    onChange={updateField}
                    className="input"
                  />
                </div>
              )}

              <h3 className="col-span-2 font-semibold text-gray-700">School Details</h3>

              <input
                name="name"
                maxLength={100}
                placeholder="Name *"
                value={form.name}
                onChange={updateField}
                className="input"
              />

              <input
                name="displayName"
                maxLength={100}
                placeholder="Display Name"
                value={form.displayName}
                onChange={updateField}
                className="input"
              />

              {/* Show schoolCode only in edit mode */}

              {editId && (

                <input
                  value={form.schoolCode}
                  disabled
                  className="input bg-gray-100 col-span-2"
                />

              )}

              <input
                name="board"
                maxLength={50}
                placeholder="Board *"
                value={form.board}
                onChange={updateField}
                className="input"
              />

              <input
                name="medium"
                maxLength={50}
                placeholder="Medium *"
                value={form.medium}
                onChange={updateField}
                className="input"
              />

              <input
                name="city"
                maxLength={50}
                placeholder="City *"
                value={form.city}
                onChange={updateField}
                className="input"
              />

              <input
                name="state"
                maxLength={50}
                placeholder="State *"
                value={form.state}
                onChange={updateField}
                className="input"
              />

              <input
                name="contactEmail"
                type="email"
                maxLength={100}
                placeholder="School Email *"
                value={form.contactEmail}
                onChange={updateField}
                className="input"
              />

              <input
                name="contactNumber"
                maxLength={15}
                placeholder="Phone"
                value={form.contactNumber}
                onChange={updateField}
                className="input"
              />

              <input
                name="pincode"
                maxLength={10}
                placeholder="Pincode"
                value={form.pincode}
                onChange={updateField}
                className="input"
              />

              <input
                name="website"
                maxLength={150}
                placeholder="Website"
                value={form.website}
                onChange={updateField}
                className="input col-span-2"
              />

              <textarea
                name="address"
                maxLength={300}
                placeholder="Address"
                value={form.address}
                onChange={updateField}
                className="input col-span-2"
              />

              <textarea
                name="description"
                maxLength={500}
                placeholder="Description"
                value={form.description}
                onChange={updateField}
                className="input col-span-2"
              />

            </div>

            {/* Save */}

            <div className="pt-4 text-right">

              <button
                onClick={saveSchool}
                className="bg-blue-600 text-white px-6 py-2 rounded"
              >
                {editId ? "Update School" : "Create & Onboard"}
              </button>

            </div>

          </div>

        </div>
      )}

    </div>
  );
}
