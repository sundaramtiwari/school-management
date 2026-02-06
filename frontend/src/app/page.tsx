"use client";

import { useEffect, useState } from "react";
import { schoolApi } from "@/lib/schoolApi";
import { useAuth } from "@/context/AuthContext";

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

  const { user } = useAuth();
  const [schools, setSchools] = useState<School[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const canDelete = user?.role === "SUPER_ADMIN" || user?.role === "PLATFORM_ADMIN";

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

  async function deleteSchool(id: number) {
    if (!confirm("Are you sure you want to delete this school? This action cannot be undone.")) return;

    try {
      await schoolApi.delete(id);
      loadSchools();
    } catch (e: any) {
      alert("Delete failed: " + (e.response?.data?.message || e.message));
    }
  }

  return (
    <div className="space-y-6">

      {/* Header */}
      <h1 className="text-2xl font-bold">Schools</h1>




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
                {canDelete && <th className="p-3 text-center">Actions</th>}
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
                  {canDelete && (
                    <td className="p-3 text-center">
                      <button
                        onClick={() => deleteSchool(s.id)}
                        className="text-red-600 hover:text-red-800 font-medium"
                      >
                        Delete
                      </button>
                    </td>
                  )}
                </tr>
              ))}

            </tbody>

          </table>

        </div>
      )}

    </div>
  );
}
