"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Modal from "@/components/ui/Modal";
import { api, extractApiError } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";

type Student = {
  id: number;
  firstName: string;
  lastName: string;
};

type Session = {
  id: number;
  name: string;
};

type SchoolClass = {
  id: number;
  name: string;
  section?: string;
};

type LedgerEntry = {
  pending?: number | string;
  totalPending?: number | string;
};

type Props = {
  selectedStudents: Student[];
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
};

export default function PromotionModal({
  selectedStudents,
  isOpen,
  onClose,
  onSuccess,
}: Props) {
  const { showToast } = useToast();
  const [selectedStudentIds, setSelectedStudentIds] = useState<Set<number>>(new Set());
  const [sessions, setSessions] = useState<Session[]>([]);
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [targetSessionId, setTargetSessionId] = useState<number | null>(null);
  const [targetClassId, setTargetClassId] = useState<number | null>(null);
  const [promotionType, setPromotionType] = useState<"PROMOTE" | "REPEAT">("PROMOTE");
  const [loading, setLoading] = useState(false);
  const [warningVisible, setWarningVisible] = useState(false);
  const [results, setResults] = useState<any[] | null>(null);

  const selectedStudentIdsArray = useMemo(
    () => Array.from(selectedStudentIds),
    [selectedStudentIds]
  );

  const resetState = useCallback(() => {
    setSelectedStudentIds(new Set(selectedStudents.map((s) => s.id)));
    setTargetSessionId(null);
    setTargetClassId(null);
    setPromotionType("PROMOTE");
    setWarningVisible(false);
    setClasses([]);
    setResults(null);
  }, [selectedStudents]);

  const loadSessions = useCallback(async () => {
    try {
      const res = await api.get("/api/academic-sessions");
      setSessions(Array.isArray(res.data) ? res.data : []);
    } catch {
      showToast("Failed to load sessions", "error");
    }
  }, [showToast]);

  const loadClassesBySession = useCallback(async (sessionId: number) => {
    try {
      const res = await api.get(`/api/classes/mine/session/${sessionId}?page=0&size=200`);
      setClasses(res.data?.content || []);
    } catch {
      showToast("Failed to load classes for session", "error");
      setClasses([]);
    }
  }, [showToast]);

  useEffect(() => {
    if (!isOpen) return;
    resetState();
    void loadSessions();
  }, [isOpen, resetState, loadSessions]);

  useEffect(() => {
    if (!isOpen || !targetSessionId) {
      setClasses([]);
      return;
    }
    void loadClassesBySession(targetSessionId);
  }, [isOpen, targetSessionId, loadClassesBySession]);

  const toggleStudent = useCallback((studentId: number) => {
    setSelectedStudentIds((prev) => {
      const next = new Set(prev);
      if (next.has(studentId)) next.delete(studentId);
      else next.add(studentId);
      return next;
    });
  }, []);

  const hasPendingFee = useCallback(async (studentId: number) => {
    const res = await api.get(`/api/students/${studentId}/ledger`);
    const entries: LedgerEntry[] = Array.isArray(res.data) ? res.data : [];
    return entries.some((entry) => {
      const pendingValue = entry.totalPending ?? entry.pending ?? 0;
      return Number(pendingValue) > 0;
    });
  }, []);

  const closeAndReset = useCallback(() => {
    onClose();
    setSelectedStudentIds(new Set());
    setTargetSessionId(null);
    setTargetClassId(null);
    setPromotionType("PROMOTE");
    setWarningVisible(false);
    setClasses([]);
    setResults(null);
  }, [onClose]);

  const handleSubmit = useCallback(async () => {
    if (selectedStudentIdsArray.length === 0 || !targetSessionId || !targetClassId) {
      showToast("Select students, session and class", "warning");
      return;
    }

    try {
      setLoading(true);

      const pendingChecks = await Promise.all(
        selectedStudentIdsArray.map((studentId) => hasPendingFee(studentId))
      );
      const hasAnyPending = pendingChecks.some(Boolean);

      if (hasAnyPending && !warningVisible) {
        setWarningVisible(true);
        return;
      }

      const response = await api.post("/api/promotions", {
        studentIds: selectedStudentIdsArray,
        targetSessionId,
        targetClassId,
        promotionType,
        remarks: "",
      });

      showToast("Students promoted successfully", "success");
      onSuccess();
      setResults(response.data);
    } catch (err) {
      showToast(extractApiError(err, "Failed to promote students"), "error");
    } finally {
      setLoading(false);
    }
  }, [
    selectedStudentIdsArray,
    targetSessionId,
    targetClassId,
    promotionType,
    hasPendingFee,
    warningVisible,
    showToast,
    onSuccess,
    closeAndReset,
  ]);

  return (
    <Modal
      isOpen={isOpen}
      onClose={closeAndReset}
      title="Promote Students"
      maxWidth="max-w-2xl"
      footer={
        <div className="flex gap-2">
          {results ? (
            <button
              onClick={closeAndReset}
              className="px-4 py-2 rounded-lg bg-gray-900 text-white font-semibold flex-1"
            >
              Done
            </button>
          ) : (
            <>
              <button
                onClick={closeAndReset}
                className="px-4 py-2 rounded-lg border border-gray-300 text-gray-700"
              >
                Cancel
              </button>
              <button
                onClick={handleSubmit}
                disabled={loading || selectedStudentIdsArray.length === 0 || !targetSessionId || !targetClassId}
                className="px-4 py-2 rounded-lg bg-blue-600 text-white font-semibold disabled:bg-gray-400"
              >
                {loading ? "Processing..." : warningVisible ? "Continue Promotion" : "Promote"}
              </button>
            </>
          )}
        </div>
      }
    >
      {results ? (
        <div className="space-y-4">
          <div className="bg-gray-50 border rounded-lg p-4 font-medium flex justify-between">
            <span className="text-green-600 font-bold">{results.filter((r: any) => r.promotionType === 'PROMOTE' || r.promotionType === 'PROMOTED').length} promoted</span>
            <span className="text-red-600 font-bold">{results.filter((r: any) => r.promotionType === 'REPEAT' || r.promotionType === 'DEMOTED').length} failed</span>
            <span className="text-blue-600 font-bold">{results.filter((r: any) => r.promotionType === 'GRADUATED').length} graduated</span>
          </div>
          <div className="max-h-64 overflow-y-auto border rounded-lg p-2 space-y-2 text-sm">
            {results.map((r: any, idx: number) => {
              const studentName = selectedStudents.find(s => s.id === r.studentId)?.firstName || "Student " + r.studentId;
              return (
                <div key={idx} className="flex justify-between items-center p-2 bg-white border-b last:border-0">
                  <span className="font-semibold text-gray-700">{studentName}</span>
                  <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${r.promotionType === 'PROMOTE' || r.promotionType === 'PROMOTED' || r.promotionType === 'GRADUATED' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                    {r.promotionType}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      ) : (
        <div className="space-y-5">
          <div>
            <h3 className="text-sm font-bold text-gray-700 mb-2">Selected Students</h3>
            <div className="max-h-40 overflow-auto border rounded-lg p-2 space-y-1">
              {selectedStudents.map((student) => (
                <label key={student.id} className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={selectedStudentIds.has(student.id)}
                    onChange={() => toggleStudent(student.id)}
                  />
                  <span>{student.firstName} {student.lastName}</span>
                </label>
              ))}
            </div>
          </div>

          <div>
            <h3 className="text-sm font-bold text-gray-700 mb-2">Target Session</h3>
            <select
              value={targetSessionId ?? ""}
              onChange={(e) => {
                setTargetSessionId(e.target.value ? Number(e.target.value) : null);
                setTargetClassId(null);
              }}
              className="input-ref"
            >
              <option value="">Select Session</option>
              {sessions.map((session) => (
                <option key={session.id} value={session.id}>
                  {session.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <h3 className="text-sm font-bold text-gray-700 mb-2">Target Class</h3>
            <select
              value={targetClassId ?? ""}
              onChange={(e) => setTargetClassId(e.target.value ? Number(e.target.value) : null)}
              className="input-ref"
              disabled={!targetSessionId}
            >
              <option value="">Select Class</option>
              {classes.map((schoolClass) => (
                <option key={schoolClass.id} value={schoolClass.id}>
                  {schoolClass.name} {schoolClass.section || ""}
                </option>
              ))}
            </select>
          </div>

          <div>
            <h3 className="text-sm font-bold text-gray-700 mb-2">Promotion Type</h3>
            <div className="flex gap-4 text-sm">
              <label className="flex items-center gap-2">
                <input
                  type="radio"
                  checked={promotionType === "PROMOTE"}
                  onChange={() => setPromotionType("PROMOTE")}
                />
                PROMOTE
              </label>
              <label className="flex items-center gap-2">
                <input
                  type="radio"
                  checked={promotionType === "REPEAT"}
                  onChange={() => setPromotionType("REPEAT")}
                />
                REPEAT
              </label>
            </div>
          </div>

          {warningVisible && (
            <div className="rounded-lg border border-amber-300 bg-amber-50 text-amber-800 text-sm p-3">
              Some students have pending fees from previous sessions. Promotion will not clear those dues.
            </div>
          )}
        </div>
      )}
    </Modal>
  );
}
