"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";

type School = {
  id: number;
  name: string;
  displayName: string;
  schoolCode: string;
};

export default function SchoolsPage() {
  const [schools, setSchools] = useState<School[]>([]);

  useEffect(() => {
    api.get("/schools")
      .then((res) => setSchools(res.data.content))
      .catch(console.error);
  }, []);

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Schools</h1>

      <table className="w-full bg-white border">
        <thead>
          <tr className="bg-gray-200">
            <th className="p-2">ID</th>
            <th className="p-2">Name</th>
            <th className="p-2">Code</th>
          </tr>
        </thead>

        <tbody>
          {schools.map((s) => (
            <tr key={s.id} className="border-t">
              <td className="p-2">{s.id}</td>
              <td className="p-2">{s.name}</td>
              <td className="p-2">{s.schoolCode}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

