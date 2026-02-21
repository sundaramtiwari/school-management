"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { canMutateFinance } from "@/lib/permissions";
import { useToast } from "@/components/ui/Toast";
import { Skeleton, TableSkeleton } from "@/components/ui/Skeleton";
import { useAuth } from "@/context/AuthContext";
import { useSession } from "@/context/SessionContext";

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
  const { sessions, currentSession } = useSession();
  const canManageFinance = canMutateFinance(user?.role);

  const [selectedSessionId, setSelectedSessionId] = useState<string>("");

  useEffect(() => {
    if (currentSession?.id && !selectedSessionId) {
      setSelectedSessionId(currentSession.id.toString());
    }
  }, [currentSession, selectedSessionId]);

  const [defaulters, setDefaulters] = useState<Defaulter[]>([]);
  const [loading, setLoading] = useState(true);

  // Pagination
  const [page, setPage] = useState(0);
  const size = 25;
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Filters
  const [selectedClass, setSelectedClass] = useState<string>("");
  const [minAmount, setMinAmount] = useState<string>("");
  const [minDays, setMinDays] = useState<string>("");

  // Search
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [debouncedSearch, setDebouncedSearch] = useState<string>("");

  const [classes, setClasses] = useState<Array<{ id: number; name: string; section: string }>>([]);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchTerm);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  useEffect(() => {
    setPage(0);
  }, [selectedClass, minAmount, minDays, selectedSessionId]);

  const loadClasses = useCallback(async () => {
    try {
      const classesRes = await api.get("/api/classes/mine?size=100");
      setClasses(classesRes.data?.content || []);
    } catch {
      showToast("Failed to load classes", "error");
    }
  }, [showToast]);

  const loadDefaulters = useCallback(async () => {
    try {
      setLoading(true);
      const params = new URLSearchParams();
      params.append("page", page.toString());
      params.append("size", size.toString());

      if (debouncedSearch) params.append("search", debouncedSearch);
      if (selectedClass) params.append("classId", selectedClass);
      if (minAmount) params.append("minAmountDue", minAmount);
      if (minDays) params.append("minDaysOverdue", minDays);
      if (selectedSessionId) params.append("sessionId", selectedSessionId);

      const res = await api.get(`/api/fees/defaulters?${params.toString()}`);

      setDefaulters(res.data?.content || []);
      setTotalElements(res.data?.totalElements || 0);
      setTotalPages(res.data?.totalPages || 0);
    } catch (err: unknown) {
      showToast("Failed to load fee defaulters", "error");
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [page, size, debouncedSearch, selectedClass, minAmount, minDays, selectedSessionId, showToast]);

  useEffect(() => {
    loadClasses();
  }, [loadClasses]);

  useEffect(() => {
    loadDefaulters();
  }, [loadDefaulters]);

  function clearFilters() {
    setSelectedClass("");
    setMinAmount("");
    setMinDays("");
    setSearchTerm("");
  }

  async function exportToExcel() {
    try {
      showToast("Exporting data...", "info");
      const params = new URLSearchParams();
      if (debouncedSearch) params.append("search", debouncedSearch);
      if (selectedClass) params.append("classId", selectedClass);
      if (minAmount) params.append("minAmountDue", minAmount);
      if (minDays) params.append("minDaysOverdue", minDays);
      if (selectedSessionId) params.append("sessionId", selectedSessionId);

      const res = await api.get(`/api/fees/defaulters/export?${params.toString()}`);
      const exportData: Defaulter[] = res.data || [];

      if (exportData.length === 0) {
        showToast("No data to export", "warning");
        return;
      }

      const headers = ["Student Name", "Admission No", "Class", "Amount Due", "Days Overdue", "Last Payment", "Parent Contact"];
      const rows = exportData.map(d => [
        `"${d.studentName}"`,
        `"${d.admissionNumber}"`,
        `"${d.className}-${d.classSection}"`,
        d.amountDue,
        d.daysOverdue,
        d.lastPaymentDate || "Never",
        `"${d.parentContact}"`
      ]);

      const csv = [
        headers.join(","),
        ...rows.map(row => row.join(","))
      ].join("\n");

      const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `fee-defaulters-${new Date().toISOString().split('T')[0]}.csv`;
      a.click();
      URL.revokeObjectURL(url);

      showToast("Exported to CSV successfully", "success");
    } catch (err: any) {
      if (err.response?.data?.message) {
        showToast(err.response.data.message, "error");
      } else {
        showToast("Export failed", "error");
      }
    }
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

  const totalDue = defaulters.reduce((sum, d) => sum + d.amountDue, 0);

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
          disabled={totalElements === 0}
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
                <p className="text-3xl font-bold text-gray-900 mt-2">{totalElements}</p>
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
              <p className="text-xs text-gray-400 mt-1">Outstanding fees (this page)</p>
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
                  {defaulters.filter(d => d.daysOverdue > 60).length}
                </p>
              )}
              <p className="text-xs text-gray-400 mt-1">&gt; 60 days overdue (this page)</p>
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

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
          <div>
            <label className="block text-xs font-bold uppercase text-gray-400 mb-2">Session</label>
            <select
              value={selectedSessionId}
              onChange={e => setSelectedSessionId(e.target.value)}
              className="input w-full font-bold"
            >
              <option value="">All Sessions</option>
              {sessions.map(s => (
                <option key={s.id} value={s.id}>
                  {s.name} {s.isActive ? "(Active)" : ""}
                </option>
              ))}
            </select>
          </div>

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
                <option key={c.id} value={c.id}>
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
        ) : defaulters.length === 0 ? (
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
              {defaulters.map(defaulter => (
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

      {/* Pagination Controls */}
      {!loading && defaulters.length > 0 && (
        <div className="flex items-center justify-between bg-white px-4 py-3 border rounded-2xl shadow-sm sm:px-6 mt-4">
          <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
            <div>
              <p className="text-sm text-gray-700">
                Showing <span className="font-medium">{page * size + 1}</span> to{" "}
                <span className="font-medium">
                  {Math.min((page + 1) * size, totalElements)}
                </span>{" "}
                of <span className="font-medium">{totalElements}</span> results
              </p>
            </div>
            <div>
              <nav className="isolate inline-flex -space-x-px rounded-md shadow-sm" aria-label="Pagination">
                <button
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                  className="relative inline-flex items-center rounded-l-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0 disabled:opacity-50"
                  aria-label="Previous page"
                >
                  <span className="sr-only">Previous</span>
                  <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                    <path fillRule="evenodd" d="M12.79 5.23a.75.75 0 01-.02 1.06L8.832 10l3.938 3.71a.75.75 0 11-1.04 1.08l-4.5-4.25a.75.75 0 010-1.08l4.5-4.25a.75.75 0 011.06.02z" clipRule="evenodd" />
                  </svg>
                </button>
                <div className="relative inline-flex items-center px-4 py-2 text-sm font-semibold text-gray-900 ring-1 ring-inset ring-gray-300 focus:z-20 focus:outline-offset-0">
                  Page {page + 1} of {totalPages}
                </div>
                <button
                  onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                  disabled={page >= totalPages - 1}
                  className="relative inline-flex items-center rounded-r-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0 disabled:opacity-50"
                  aria-label="Next page"
                >
                  <span className="sr-only">Next</span>
                  <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                    <path fillRule="evenodd" d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z" clipRule="evenodd" />
                  </svg>
                </button>
              </nav>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
