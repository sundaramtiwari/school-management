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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [name, setName] = useState("");

  useEffect(() => {
    loadSchools();
  }, []);

  async function loadSchools() {
    try {
      setLoading(true);
      const res = await schoolApi.list();
      setSchools(res.data.content);
    } catch (e) {
      setError("Failed to load schools");
    } finally {
      setLoading(false);
    }
  }

  async function createSchool() {
    if (!name) return;

    try {
      await schoolApi.create({
        name,
        displayName: name,
        board: "CBSE",
        schoolCode: "AUTO-" + Date.now(),
        city: "Varanasi",
        state: "UP",
      });

      setName("");
      loadSchools();

    } catch {
      alert("Failed to create school");
    }
  }

  return (
    <div className="space-y-6">

      {/* Header */}
      <h1 className="text-2xl font-bold">Schools</h1>


      {/* Create */}
      <div className="flex gap-2">

        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="School name"
          className="border px-3 py-2 rounded w-64"
        />

        <button
          onClick={createSchool}
          className="bg-blue-600 text-white px-4 py-2 rounded"
        >
          Add
        </button>

      </div>


      {/* States */}
      {loading && <p>Loading...</p>}
      {error && <p className="text-red-500">{error}</p>}


      {/* Table */}
      {!loading && !error && (

        <div className="bg-white border rounded">

          <table className="w-full text-sm">

            <thead className="bg-gray-100 text-left">
              <tr>
                <th className="p-3">Name</th>
                <th className="p-3">Code</th>
                <th className="p-3">Board</th>
                <th className="p-3">City</th>
              </tr>
            </thead>

            <tbody>

              {schools.map((s) => (
                <tr
                  key={s.id}
                  className="border-t hover:bg-gray-50"
                >
                  <td className="p-3">{s.name}</td>
                  <td className="p-3">{s.schoolCode}</td>
                  <td className="p-3">{s.board}</td>
                  <td className="p-3">{s.city}</td>
                </tr>
              ))}

            </tbody>

          </table>

        </div>
      )}

    </div>
  );
}
