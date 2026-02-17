"use client";

import { useEffect, useState } from "react";
import { useSession } from "@/context/SessionContext";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { Skeleton } from "@/components/ui/Skeleton";

type RecentPayment = {
  id: number;
  studentName: string;
  amount: number;
  paymentMode: string;
  paymentDate: string;
};

type Defaulter = {
  studentId: number;
  studentName: string;
  className: string;
  amountDue: number;
  daysPending: number;
};

export default function AccountantDashboard() {
  const router = useRouter();
  const { user } = useAuth();
  const { currentSession, isSessionLoading: sessionLoading } = useSession();
  const [stats, setStats] = useState({
    collectedToday: 0,
    transactionsToday: 0,
    collectedThisMonth: 0,
    pendingDues: 0,
    defaulterCount: 0,
  });
  const [recentPayments, setRecentPayments] = useState<RecentPayment[]>([]);
  const [topDefaulters, setTopDefaulters] = useState<Defaulter[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!sessionLoading && currentSession) loadAccountantDashboard();
  }, [sessionLoading, currentSession?.id]);

  async function loadAccountantDashboard() {
    try {
      setLoading(true);

      // Load fee statistics
      const [statsRes, paymentsRes, defaultersRes] = await Promise.all([
        api.get(`/api/fees/summary/stats?sessionId=${currentSession?.id}`).catch(() => ({
          data: {
            collectedToday: 0,
            transactionsToday: 0,
            collectedThisMonth: 0,
            pendingTotal: 0
          }
        })),
        api.get('/api/fees/payments/recent?limit=5').catch(() => ({ data: [] })),
        api.get('/api/fees/defaulters?limit=5').catch(() => ({ data: [] })),
      ]);

      setStats({
        collectedToday: statsRes.data.collectedToday || 0,
        transactionsToday: statsRes.data.transactionsToday || 0,
        collectedThisMonth: statsRes.data.collectedThisMonth || 0,
        pendingDues: statsRes.data.pendingTotal || 0,
        defaulterCount: statsRes.data.defaulterCount || 0,
      });

      setRecentPayments(paymentsRes.data || []);
      setTopDefaulters(defaultersRes.data || []);

    } catch (err) {
      console.error("Failed to load accountant dashboard", err);
    } finally {
      setLoading(false);
    }
  }

  const quickActions = [
    {
      title: "Collect Fee",
      icon: "💵",
      color: "green",
      href: "/fees/collect",
      description: "Record payment"
    },
    {
      title: "Generate Challan",
      icon: "📄",
      color: "blue",
      href: "/fees/structures",
      description: "Create fee slip"
    },
    {
      title: "View Defaulters",
      icon: "⚠️",
      color: "red",
      href: "/fees/defaulters",
      description: "Pending dues"
    },
    {
      title: "Monthly Report",
      icon: "📊",
      color: "purple",
      href: "/fees/reports",
      description: "Download report"
    },
  ];

  const formatCurrency = (amount: number) => {
    return `₹ ${amount.toLocaleString('en-IN')}`;
  };

  if (!sessionLoading && !currentSession) {
    return (
      <div className="flex flex-col items-center justify-center py-20 bg-gray-50 rounded-3xl border-2 border-dashed border-gray-200">
        <div className="w-20 h-20 bg-green-100 text-green-600 rounded-full flex items-center justify-center text-4xl mb-6 shadow-sm">
          💰
        </div>
        <h2 className="text-2xl font-bold text-gray-800 mb-2">Academic Session Required</h2>
        <p className="text-gray-500 max-w-md text-center mb-8">
          The finance dashboard is restricted until an academic session is initialized by the school administrator.
        </p>
        <div className="bg-white px-6 py-4 rounded-xl border flex items-center gap-3 text-sm text-gray-600">
          <span className="text-xl">ℹ️</span>
          Wait for your administrator to set up the current year.
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Welcome Header */}
      <header className="bg-gradient-to-r from-green-600 to-emerald-600 text-white p-8 rounded-2xl shadow-lg">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 bg-white/20 rounded-full flex items-center justify-center text-3xl">
            💰
          </div>
          <div>
            <h1 className="text-3xl font-bold">Finance Dashboard</h1>
            <p className="text-green-100 mt-1">Fee Collection & Management</p>
            <p className="text-green-200 text-sm mt-1">
              {new Date().toLocaleDateString('en-IN', {
                weekday: 'long',
                year: 'numeric',
                month: 'long',
                day: 'numeric'
              })}
            </p>
          </div>
        </div>
      </header>

      {/* Stats Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6">
        <div className="bg-white p-6 rounded-2xl border shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase">Today</p>
              {loading ? (
                <Skeleton className="h-10 w-24 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">
                  {formatCurrency(stats.collectedToday)}
                </p>
              )}
              <p className="text-xs text-gray-400 mt-1">{stats.transactionsToday} transactions</p>
            </div>
            <div className="w-12 h-12 bg-green-500 text-white rounded-xl flex items-center justify-center text-xl">
              💵
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase">This Month</p>
              {loading ? (
                <Skeleton className="h-10 w-24 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">
                  {formatCurrency(stats.collectedThisMonth)}
                </p>
              )}
              <p className="text-xs text-gray-400 mt-1">Collected</p>
            </div>
            <div className="w-12 h-12 bg-emerald-500 text-white rounded-xl flex items-center justify-center text-xl">
              📈
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase">Pending</p>
              {loading ? (
                <Skeleton className="h-10 w-24 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">
                  {formatCurrency(stats.pendingDues)}
                </p>
              )}
              <p className="text-xs text-gray-400 mt-1">Outstanding dues</p>
            </div>
            <div className="w-12 h-12 bg-orange-500 text-white rounded-xl flex items-center justify-center text-xl">
              ⏰
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase">Defaulters</p>
              {loading ? (
                <Skeleton className="h-10 w-16 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">{stats.defaulterCount}</p>
              )}
              <p className="text-xs text-gray-400 mt-1">Students with dues</p>
            </div>
            <div className="w-12 h-12 bg-red-500 text-white rounded-xl flex items-center justify-center text-xl">
              ⚠️
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 uppercase">Collection Rate</p>
              {loading ? (
                <Skeleton className="h-10 w-16 mt-2" />
              ) : (
                <p className="text-3xl font-bold text-gray-900 mt-2">
                  {stats.pendingDues > 0
                    ? Math.round((stats.collectedThisMonth / (stats.collectedThisMonth + stats.pendingDues)) * 100)
                    : 100}%
                </p>
              )}
              <p className="text-xs text-gray-400 mt-1">This month</p>
            </div>
            <div className="w-12 h-12 bg-blue-500 text-white rounded-xl flex items-center justify-center text-xl">
              📊
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Recent Payments */}
        <div className="bg-white p-8 rounded-2xl border shadow-sm">
          <h3 className="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
            <span>💳</span> Recent Payments
          </h3>
          <div className="space-y-3">
            {loading ? (
              <>
                <Skeleton className="h-16 w-full" />
                <Skeleton className="h-16 w-full" />
                <Skeleton className="h-16 w-full" />
              </>
            ) : recentPayments.length === 0 ? (
              <div className="text-center py-12 text-gray-400">
                <p className="text-4xl mb-3">💳</p>
                <p className="text-sm">No payments recorded today</p>
              </div>
            ) : (
              recentPayments.map((payment) => (
                <div
                  key={payment.id}
                  className="flex items-center justify-between p-4 border rounded-xl hover:bg-gray-50"
                >
                  <div className="flex-1">
                    <p className="font-bold text-gray-800">{payment.studentName}</p>
                    <p className="text-sm text-gray-500">
                      {payment.paymentMode} • {new Date(payment.paymentDate).toLocaleDateString('en-IN')}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="font-bold text-green-600">{formatCurrency(payment.amount)}</p>
                    <button
                      onClick={() => window.location.href = `/fees/payments/${payment.id}/receipt`}
                      className="text-xs text-blue-600 hover:text-blue-800 mt-1"
                    >
                      Download Receipt
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
          {recentPayments.length > 0 && (
            <button
              onClick={() => window.location.href = '/fees/collect'}
              className="w-full mt-4 py-2 text-center text-blue-600 hover:text-blue-800 font-semibold text-sm"
            >
              View All Payments →
            </button>
          )}
        </div>

        {/* Top Defaulters & Quick Actions */}
        <div className="space-y-8">
          {/* Top Defaulters */}
          <div className="bg-white p-8 rounded-2xl border shadow-sm">
            <h3 className="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
              <span>⚠️</span> Top Defaulters
            </h3>
            <div className="space-y-3">
              {loading ? (
                <>
                  <Skeleton className="h-16 w-full" />
                  <Skeleton className="h-16 w-full" />
                  <Skeleton className="h-16 w-full" />
                </>
              ) : topDefaulters.length === 0 ? (
                <div className="text-center py-12 text-gray-400">
                  <p className="text-4xl mb-3">✓</p>
                  <p className="text-sm font-semibold text-green-600">All Clear!</p>
                  <p className="text-xs mt-1">No pending dues</p>
                </div>
              ) : (
                topDefaulters.map((defaulter, i) => (
                  <div
                    key={i}
                    className="flex items-center justify-between p-4 bg-red-50 border border-red-100 rounded-xl"
                  >
                    <div className="flex-1">
                      <p className="font-bold text-gray-800">{defaulter.studentName}</p>
                      <p className="text-sm text-gray-600">{defaulter.className}</p>
                      <p className="text-xs text-red-600 mt-1">
                        {defaulter.daysPending} days overdue
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="font-bold text-red-600">{formatCurrency(defaulter.amountDue)}</p>
                      <button
                        onClick={() => window.location.href = `/fees/collect?student=${defaulter.studentId}`}
                        className="text-xs text-blue-600 hover:text-blue-800 mt-1"
                      >
                        Collect →
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
            {topDefaulters.length > 0 && (
              <button
                onClick={() => window.location.href = '/fees/defaulters'}
                className="w-full mt-4 py-2 text-center text-red-600 hover:text-red-800 font-semibold text-sm"
              >
                View All Defaulters ({stats.defaulterCount}) →
              </button>
            )}
          </div>

          {/* Quick Actions */}
          <div className="bg-white p-8 rounded-2xl border shadow-sm">
            <h3 className="text-xl font-bold text-gray-800 mb-6">Quick Actions</h3>
            <div className="grid grid-cols-2 gap-4">
              {quickActions.map((action, i) => (
                <button
                  key={i}
                  onClick={() => window.location.href = action.href}
                  className={`
                    p-4 rounded-xl border-2 border-gray-100
                    hover:border-${action.color}-400 hover:bg-${action.color}-50
                    transition-all text-left group bg-gray-50
                  `}
                >
                  <span className="block text-2xl mb-2">{action.icon}</span>
                  <span className="font-semibold text-gray-700 text-sm block">
                    {action.title}
                  </span>
                  <span className="text-xs text-gray-500 mt-1 block">
                    {action.description}
                  </span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Alerts */}
      {stats.defaulterCount > 10 && (
        <div className="bg-red-50 border border-red-200 rounded-2xl p-6 flex items-start gap-4">
          <span className="text-3xl">🚨</span>
          <div className="flex-1">
            <h3 className="font-bold text-red-800 text-lg">High Defaulter Count Alert</h3>
            <p className="text-red-600 text-sm mt-1">
              {stats.defaulterCount} students have pending dues totaling {formatCurrency(stats.pendingDues)}.
              Consider sending fee reminders or scheduling collection drives.
            </p>
            <button
              onClick={() => window.location.href = '/fees/defaulters'}
              className="mt-3 bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition font-semibold text-sm"
            >
              View Defaulter List
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
