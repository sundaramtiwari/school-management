"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Modal from "@/components/ui/Modal";
import { api } from "@/lib/api";
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
  pending: number | string;
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
    return entries.some((entry) => Number(entry.pending) > 0);
  }, []);

  const closeAndReset = useCallback(() => {
    onClose();
    setSelectedStudentIds(new Set());
    setTargetSessionId(null);
    setTargetClassId(null);
    setPromotionType("PROMOTE");
    setWarningVisible(false);
    setClasses([]);
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

      await api.post("/api/promotions", {
        studentIds: selectedStudentIdsArray,
        targetSessionId,
        targetClassId,
        promotionType,
        remarks: "",
      });

      showToast("Students promoted successfully", "success");
      onSuccess();
      closeAndReset();
    } catch {
      showToast("Failed to promote students", "error");
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
        </div>
      }
    >
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
    </Modal>
  );
}
