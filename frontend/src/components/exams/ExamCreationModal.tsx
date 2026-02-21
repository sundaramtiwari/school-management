"use client";

import { useState } from "react";
import Modal from "@/components/ui/Modal";
import { examApi } from "@/lib/examApi";
import { useToast } from "@/components/ui/Toast";
import { useAuth } from "@/context/AuthContext";

interface ExamCreationModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    classId: number;
    sessionId: number;
}

export default function ExamCreationModal({
    isOpen,
    onClose,
    onSuccess,
    classId,
    sessionId,
}: ExamCreationModalProps) {
    const { user } = useAuth();
    const { showToast } = useToast();
    const [isSaving, setIsSaving] = useState(false);

    const [form, setForm] = useState({
        name: "",
        examType: "UT", // Default to Unit Test
        startDate: "",
        endDate: "",
    });

    async function handleSave() {
        if (!form.name || !form.examType || !user?.schoolId) {
            showToast("Please fill all fields", "warning");
            return;
        }

        try {
            setIsSaving(true);
            await examApi.create({
                schoolId: user.schoolId,
                classId,
                sessionId,
                name: form.name,
                examType: form.examType,
                startDate: form.startDate || undefined,
                endDate: form.endDate || undefined,
            });
            showToast("Exam created successfully!", "success");
            onSuccess();
        } catch (err: unknown) {
            const msg = typeof err === "object" && err !== null && "message" in err
                ? String((err as { message?: string }).message || "Failed to create exam")
                : "Failed to create exam";
            showToast(msg, "error");
        } finally {
            setIsSaving(false);
        }
    }

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title="Create New Exam"
            footer={
                <div className="flex gap-2">
                    <button
                        onClick={onClose}
                        className="px-6 py-2 rounded-xl border font-medium text-gray-600 hover:bg-gray-50 transition-all"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleSave}
                        disabled={isSaving}
                        className="px-8 py-2 rounded-xl bg-blue-600 text-white font-bold shadow-lg hover:bg-blue-700 disabled:bg-gray-400 transition-all"
                    >
                        {isSaving ? "Creating..." : "Create Exam"}
                    </button>
                </div>
            }
        >
            <div className="space-y-4">
                <div className="space-y-1">
                    <label className="text-xs font-bold text-gray-400 uppercase ml-1">Exam Name</label>
                    <input
                        placeholder="e.g. First Unit Test, Half Yearly 2025"
                        value={form.name}
                        onChange={(e) => setForm({ ...form, name: e.target.value })}
                        className="input-ref"
                    />
                </div>

                <div className="space-y-1">
                    <label className="text-xs font-bold text-gray-400 uppercase ml-1">Exam Type</label>
                    <select
                        value={form.examType}
                        onChange={(e) => setForm({ ...form, examType: e.target.value })}
                        className="input-ref"
                    >
                        <option value="UT">Unit Test (UT)</option>
                        <option value="MID">Mid Term (MID)</option>
                        <option value="FINAL">Final Exam (FINAL)</option>
                        <option value="PERIODIC">Periodic Test</option>
                    </select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1">
                        <label className="text-xs font-bold text-gray-400 uppercase ml-1">Start Date</label>
                        <input
                            type="date"
                            value={form.startDate}
                            onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                            className="input-ref"
                        />
                    </div>
                    <div className="space-y-1">
                        <label className="text-xs font-bold text-gray-400 uppercase ml-1">End Date</label>
                        <input
                            type="date"
                            value={form.endDate}
                            onChange={(e) => setForm({ ...form, endDate: e.target.value })}
                            className="input-ref"
                        />
                    </div>
                </div>

                <div className="p-4 bg-gray-50 rounded-xl border border-dashed text-xs text-gray-500">
                    <p>This exam will be created for the selected class and current academic session.</p>
                </div>
            </div>
        </Modal>
    );
}
