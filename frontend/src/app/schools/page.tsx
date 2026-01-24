"use client";

import { useEffect, useState } from "react";
import { schoolApi } from "@/lib/schoolApi";

type School = {
  id: number;
  name: string;
  displayName: string;
  board: string;
  schoolCode: string;
  city: string;
  state: string;
};

export default function SchoolsPage() {
  const [schools, setSchools] = useState<School[]>([]);
  const [showForm, setShowForm] = useState(false);

  const [form, setForm] = useState({
    name: "",
    displayName: "",
    board: "",
    schoolCode: "",
    city: "",
    state: "",
  });

  const load = () => {
    schoolApi.list().then((res) => {
      setSchools(res.data.content);
    });
  };

  useEffect(() => {
    load();
  }, []);

  const submit = async () => {
    await schoolApi.create(form);
    setShowForm(false);
    setForm({
      name: "",
      displayName: "",
      board: "",
      schoolCode: "",
      city: "",
      state: "",
    });
    load();
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">Schools</h1>

        <button
          onClick={() => setShowForm(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded"
        >
          + Add School
        </button>
      </div>

      {/* Form Modal */}
      {showForm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center">
          <div className="bg-white p-6 rounded w-[400px] space-y-3">

            <h2 className="text-lg font-semibold">New School</h2>

            {Object.keys(form).map((key) => (
              <input
                key={key}
                placeholder={key}
                className="w-full border p-2 rounded"
                value={(form as any)[key]}
                onChange={(e) =>
                  setForm({ ...form, [key]: e.target.value })
                }
              />
            ))}

            <div className="flex justify-end gap-2 pt-2">

              <button
                onClick={() => setShowForm(false)}
                className="px-3 py-1 border rounded"
              >
                Cancel
              </button>

              <button
                onClick={submit}
                className="px-3 py-1 bg-blue-600 text-white rounded"
              >
                Save
              </button>

            </div>
          </div>
        </div>
      )}

      {/* List */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {schools.map((s) => (
          <div
            key={s.id}
            className="bg-white p-4 rounded shadow"
          >
            <div className="font-semibold">{s.name}</div>
            <div className="text-sm text-gray-600">
              {s.schoolCode}
            </div>
            <div className="text-xs text-gray-500">
              {s.city}, {s.state}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
