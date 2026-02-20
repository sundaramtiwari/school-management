"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { canMutateFinance } from "@/lib/permissions";
import { useToast } from "@/components/ui/Toast";
import { Skeleton, TableSkeleton } from "@/components/ui/Skeleton";
import { useAuth } from "@/context/AuthContext";

type Defaulter = {
  studentId: number;
  studentName: string;
  admissionNumber: string;
  className: string;
  classSection: string;
  amountDue: number;
  lastPaymentDate: string | null;
  daysOverdue: number;
  parentContact: string;
};

export default function FeeDefaultersPage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const canManageFinance = canMutateFinance(user?.role);

  const [defaulters, setDefaulters] = useState<Defaulter[]>([]);
  const [filteredDefaulters, setFilteredDefaulters] = useState<Defaulter[]>([]);
  const [loading, setLoading] = useState(true);

  // Filters
  const [selectedClass, setSelectedClass] = useState<string>("");
  const [minAmount, setMinAmount] = useState<string>("");
  const [minDays, setMinDays] = useState<string>("");
  const [searchTerm, setSearchTerm] = useState<string>("");

  const [classes, setClasses] = useState<Array<{ id: number; name: string; section: string }>>([]);

  const applyFilters = useCallback(() => {
    let filtered = [...defaulters];

    // Filter by class
    if (selectedClass) {
      filtered = filtered.filter(d => `${d.className}-${d.classSection}` === selectedClass);
    }

    // Filter by minimum amount
    if (minAmount) {
      filtered = filtered.filter(d => d.amountDue >= Number(minAmount));
    }

    // Filter by minimum days overdue
    if (minDays) {
      filtered = filtered.filter(d => d.daysOverdue >= Number(minDays));
    }

    // Search by name or admission number
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(d =>
        d.studentName.toLowerCase().includes(term) ||
        d.admissionNumber.toLowerCase().includes(term)
      );
    }

    // Sort by amount due (highest first)
    filtered.sort((a, b) => b.amountDue - a.amountDue);

    setFilteredDefaulters(filtered);
  }, [defaulters, minAmount, minDays, searchTerm, selectedClass]);

  const loadClassesAndDefaulters = useCallback(async () => {
    try {
      setLoading(true);

      // Load classes for filter
      const classesRes = await api.get("/api/classes/mine?size=100");
      setClasses(classesRes.data?.content || []);

      // Load defaulters
      // Backend endpoint: GET /api/fees/defaulters
      // Expected response: Array of Defaulter objects
      const defaultersRes = await api.get("/api/fees/defaulters");
      setDefaulters(defaultersRes.data || []);

    } catch (err: unknown) {
      showToast("Failed to load fee defaulters", "error");
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    loadClassesAndDefaulters();
  }, [loadClassesAndDefaulters]);

  useEffect(() => {
    applyFilters();
  }, [applyFilters]);

  function clearFilters() {
    setSelectedClass("");
    setMinAmount("");
    setMinDays("");
    setSearchTerm("");
  }

  function exportToExcel() {
    // Convert to CSV format
    const headers = ["Student Name", "Admission No", "Class", "Amount Due", "Days Overdue", "Last Payment", "Parent Contact"];
    const rows = filteredDefaulters.map(d => [
      d.studentName,
      d.admissionNumber,
      `${d.className}-${d.classSection}`,
      d.amountDue,
      d.daysOverdue,
      d.lastPaymentDate || "Never",
      d.parentContact
    ]);

    const csv = [
      headers.join(","),
      ...rows.map(row => row.join(","))
    ].join("\n");

    // Download
    const blob = new Blob([csv], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `fee-defaulters-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);

    showToast("Exported to CSV", "success");
  }

  function goToCollectFee(studentId: number) {
    window.location.href = `/fees/collect?student=${studentId}`;
  }

  function sendReminder(defaulter: Defaulter) {
    // For now, open WhatsApp with pre-filled message
    // In future: Integrate with SMS API
    const message = encodeURIComponent(
      `Dear Parent,\n\nThis is a reminder that ${defaulter.studentName} (${defaulter.admissionNumber}) has pending school fees of ₹${defaulter.amountDue.toLocaleString('en-IN')}.\n\nPlease clear the dues at the earliest.\n\nThank you,\nSchool Administration`
    );
    const normalizedContact = defaulter.parentContact.replace(/\D/g, "");
    const waNumber = normalizedContact.startsWith("91") ? normalizedContact : `91${normalizedContact}`;
    window.open(`https://wa.me/${waNumber}?text=${message}`, '_blank');
  }

  const totalDue = filteredDefaulters.reduce((sum, d) => sum + d.amountDue, 0);

  return (
    <div className="space-y-6">
      {/* Header */}
      <header className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">Fee Defaulters</h1>
          <p className="text-gray-500">Students with pending fee payments</p>
        </div>
        <button
          onClick={exportToExcel}
          disabled={filteredDefaulters.length === 0}
          className="bg-green-600 text-white px-6 py-2.5 rounded-xl font-bold shadow-lg hover:bg-green-700 transition-all disabled:bg-gray-400"
        >
          📊 Export to Excel
        </button>
      </header>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-2xl border shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase">Total Defaulters</p>
              {loading ? (
                <Skeleton className="h-10 w-16 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">{filteredDefaulters.length}</p>
              )}
              <p className="text-xs text-gray-400 mt-1">Students with pending dues</p>
            </div>
            <div className="w-12 h-12 bg-red-500 text-white rounded-xl flex items-center justify-center text-xl">
              ⚠️
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase">Total Amount Due</p>
              {loading ? (
                <Skeleton className="h-10 w-24 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-red-600 mt-2">
                  ₹ {totalDue.toLocaleString('en-IN')}
                </p>
              )}
              <p className="text-xs text-gray-400 mt-1">Outstanding fees</p>
            </div>
            <div className="w-12 h-12 bg-orange-500 text-white rounded-xl flex items-center justify-center text-xl">
              💰
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase">Critical Cases</p>
              {loading ? (
                <Skeleton className="h-10 w-16 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">
                  {filteredDefaulters.filter(d => d.daysOverdue > 60).length}
                </p>
              )}
              <p className="text-xs text-gray-400 mt-1">&gt; 60 days overdue</p>
            </div>
            <div className="w-12 h-12 bg-yellow-500 text-white rounded-xl flex items-center justify-center text-xl">
              🚨
            </div>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white p-6 border rounded-2xl shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-bold text-gray-800">Filters</h3>
          {(selectedClass || minAmount || minDays || searchTerm) && (
            <button
              onClick={clearFilters}
              className="text-sm text-blue-600 hover:underline font-semibold"
            >
              Clear All
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div>
            <label className="block text-xs font-bold uppercase text-gray-400 mb-2">Search Student</label>
            <input
              type="text"
              placeholder="Name or Admission No"
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              className="input w-full"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-gray-400 mb-2">Class</label>
            <select
              value={selectedClass}
              onChange={e => setSelectedClass(e.target.value)}
              className="input w-full"
            >
              <option value="">All Classes</option>
              {classes.map(c => (
                <option key={c.id} value={`${c.name}-${c.section}`}>
                  {c.name}-{c.section}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-gray-400 mb-2">Min Amount Due</label>
            <input
              type="number"
              placeholder="₹ 0"
              value={minAmount}
              onChange={e => setMinAmount(e.target.value)}
              className="input w-full"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-gray-400 mb-2">Min Days Overdue</label>
            <input
              type="number"
              placeholder="0 days"
              value={minDays}
              onChange={e => setMinDays(e.target.value)}
              className="input w-full"
            />
          </div>
        </div>
      </div>

      {/* Defaulters Table */}
      <div className="bg-white border rounded-2xl shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-8">
            <TableSkeleton rows={10} cols={7} />
          </div>
        ) : filteredDefaulters.length === 0 ? (
          <div className="p-20 text-center text-gray-400">
            <p className="text-4xl mb-4">✓</p>
            <p className="font-semibold text-green-600">No fee defaulters!</p>
            <p className="text-sm mt-2">All students have cleared their fees</p>
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600 font-bold border-b">
              <tr>
                <th className="p-4 text-left">Student Details</th>
                <th className="p-4 text-center">Class</th>
                <th className="p-4 text-right">Amount Due</th>
                <th className="p-4 text-center">Days Overdue</th>
                <th className="p-4 text-center">Last Payment</th>
                <th className="p-4 text-center">Contact</th>
                <th className="p-4 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredDefaulters.map(defaulter => (
                <tr key={defaulter.studentId} className="hover:bg-gray-50 transition-colors">
                  <td className="p-4">
                    <div>
                      <p className="font-bold text-gray-800">{defaulter.studentName}</p>
                      <p className="text-xs text-gray-500">{defaulter.admissionNumber}</p>
                    </div>
                  </td>
                  <td className="p-4 text-center">
                    <span className="px-3 py-1 bg-blue-50 text-blue-600 rounded-lg text-xs font-bold">
                      {defaulter.className}-{defaulter.classSection}
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <span className="font-bold text-red-600 text-base">
                      ₹ {defaulter.amountDue.toLocaleString('en-IN')}
                    </span>
                  </td>
                  <td className="p-4 text-center">
                    <span className={`px-3 py-1 rounded-lg text-xs font-bold ${defaulter.daysOverdue > 60 ? 'bg-red-100 text-red-700' :
                        defaulter.daysOverdue > 30 ? 'bg-orange-100 text-orange-700' :
                          'bg-yellow-100 text-yellow-700'
                      }`}>
                      {defaulter.daysOverdue} days
                    </span>
                  </td>
                  <td className="p-4 text-center text-gray-600">
                    {defaulter.lastPaymentDate
                      ? new Date(defaulter.lastPaymentDate).toLocaleDateString('en-IN')
                      : <span className="text-gray-400 italic">Never</span>
                    }
                  </td>
                  <td className="p-4 text-center">
                    <a
                      href={`tel:${defaulter.parentContact}`}
                      className="text-blue-600 hover:underline font-mono text-xs"
                    >
                      {defaulter.parentContact}
                    </a>
                  </td>
                  <td className="p-4">
                    {canManageFinance ? (
                      <div className="flex gap-2 justify-center">
                        <button
                          onClick={() => goToCollectFee(defaulter.studentId)}
                          className="px-3 py-1.5 bg-green-600 text-white rounded-lg hover:bg-green-700 transition text-xs font-bold"
                        >
                          Collect
                        </button>
                        <button
                          onClick={() => sendReminder(defaulter)}
                          className="px-3 py-1.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition text-xs font-bold"
                        >
                          Remind
                        </button>
                      </div>
                    ) : (
                      <span className="text-gray-400 text-xs italic">View Only</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
