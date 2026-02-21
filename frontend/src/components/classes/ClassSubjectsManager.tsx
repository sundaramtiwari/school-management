"use client";

import { useCallback, useEffect, useState } from "react";
import { classSubjectApi, ClassSubjectData } from "@/lib/classSubjectApi";
import { subjectApi, SubjectData } from "@/lib/subjectApi";
import { api } from "@/lib/api";
import Modal from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";

interface ClassSubjectsManagerProps {
    classId: number;
}

type Teacher = {
    id: number;
    fullName: string;
};

export default function ClassSubjectsManager({ classId }: ClassSubjectsManagerProps) {
    const [assigned, setAssigned] = useState<ClassSubjectData[]>([]);
    const [loading, setLoading] = useState(true);
    const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
    const [availableSubjects, setAvailableSubjects] = useState<SubjectData[]>([]);
    const [teachers, setTeachers] = useState<Teacher[]>([]);
    const [selectedSubjectId, setSelectedSubjectId] = useState<string>("");
    const [selectedTeacherId, setSelectedTeacherId] = useState<string>("");
    const [assigning, setAssigning] = useState(false);
    const { showToast } = useToast();

    function getErrorMessage(error: unknown): string {
        if (error && typeof error === "object" && "response" in error) {
            const response = (error as { response?: { data?: { message?: string } } }).response;
            if (response?.data?.message) return response.data.message;
        }
        if (error instanceof Error) return error.message;
        return "Unknown error";
    }

    const fetchAssigned = useCallback(async () => {
        setLoading(true);
        try {
            const data = await classSubjectApi.getByClass(classId, 0, 100);
            setAssigned(data.content);
        } catch (error) {
            console.error("Failed to fetch assigned subjects", error);
            showToast("Failed to fetch assigned subjects", "error");
        } finally {
            setLoading(false);
        }
    }, [classId, showToast]);

    useEffect(() => {
        if (classId) {
            fetchAssigned();
        }
    }, [classId, fetchAssigned]);

    const openAssignModal = async () => {
        setIsAssignModalOpen(true);
        setSelectedSubjectId("");
        setSelectedTeacherId("");
        try {
            const [subjectsRes, teachersRes] = await Promise.all([
                subjectApi.getAll(0, 100, true),
                api.get<Teacher[]>("/api/teachers"),
            ]);
            const data = subjectsRes;
            setAvailableSubjects(data.content);
            setTeachers(teachersRes.data || []);
        } catch (error) {
            console.error("Failed to fetch assign modal data", error);
            showToast("Failed to fetch subjects or teachers", "error");
        }
    };

    const handleAssign = async () => {
        if (!selectedSubjectId || !selectedTeacherId) return;
        setAssigning(true);
        try {
            await classSubjectApi.assign(classId, parseInt(selectedSubjectId), parseInt(selectedTeacherId));
            showToast("Subject assigned successfully", "success");
            setIsAssignModalOpen(false);
            setSelectedSubjectId("");
            setSelectedTeacherId("");
            fetchAssigned();
        } catch (error: unknown) {
            console.error("Failed to assign subject", error);
            showToast(getErrorMessage(error) || "Failed to assign subject", "error");
        } finally {
            setAssigning(false);
        }
    };

    const handleRemove = async (id: number) => {
        if (!confirm("Are you sure you want to remove this subject from the class?")) return;
        try {
            await classSubjectApi.remove(id);
            showToast("Subject removed successfully", "success");
            fetchAssigned();
        } catch (error: unknown) {
            showToast(getErrorMessage(error) || "Failed to remove subject", "error");
        }
    };

    const footer = (
        <div className="flex justify-end gap-2">
            <button
                onClick={() => setIsAssignModalOpen(false)}
                className="px-4 py-2 border rounded-md text-gray-600 hover:bg-gray-50"
            >
                Cancel
            </button>
            <button
                onClick={handleAssign}
                disabled={assigning || !selectedSubjectId || !selectedTeacherId}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
            >
                {assigning ? "Assigning..." : "Assign"}
            </button>
        </div>
    );

    return (
        <div className="space-y-4">
            <div className="flex justify-between items-center">
                <h3 className="text-lg font-semibold">Assigned Subjects</h3>
                <button
                    onClick={openAssignModal}
                    className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
                >
                    + Assign Subject
                </button>
            </div>

            <div className="border rounded-md overflow-hidden">
                <table className="w-full text-sm text-left">
                    <thead className="bg-gray-50 text-gray-700 uppercase">
                        <tr>
                            <th className="px-6 py-3">Subject Name</th>
                            <th className="px-6 py-3">Code</th>
                            <th className="px-6 py-3">Teacher</th>
                            <th className="px-6 py-3">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr>
                                <td colSpan={4} className="px-6 py-4 text-center">Loading...</td>
                            </tr>
                        ) : assigned.length === 0 ? (
                            <tr>
                                <td colSpan={4} className="px-6 py-4 text-center">No subjects assigned.</td>
                            </tr>
                        ) : (
                            assigned.map((item) => (
                                <tr key={item.id} className="border-b hover:bg-gray-50">
                                    <td className="px-6 py-4 font-medium">{item.subjectName}</td>
                                    <td className="px-6 py-4">{item.subjectCode || "-"}</td>
                                    <td className="px-6 py-4">
                                        {item.teacherName || teachers.find((teacher) => teacher.id === item.teacherId)?.fullName || "-"}
                                    </td>
                                    <td className="px-6 py-4">
                                        <button
                                            onClick={() => handleRemove(item.id)}
                                            className="text-white bg-red-600 hover:bg-red-700 px-3 py-1 rounded transition-colors text-xs"
                                        >
                                            Remove
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            <Modal
                isOpen={isAssignModalOpen}
                onClose={() => setIsAssignModalOpen(false)}
                title="Assign Subject to Class"
                footer={footer}
            >
                <div className="grid gap-4 py-4">
                    <div className="grid grid-cols-4 items-center gap-4">
                        <label htmlFor="subject" className="text-right text-sm font-medium">
                            Subject
                        </label>
                        <div className="col-span-3">
                            <select
                                className="flex h-10 w-full rounded-md border border-gray-300 bg-transparent px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                value={selectedSubjectId}
                                onChange={(e) => setSelectedSubjectId(e.target.value)}
                            >
                                <option value="">Select Subject</option>
                                {availableSubjects.map((s) => (
                                    <option key={s.id} value={s.id}>
                                        {s.name} ({s.code})
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>
                    <div className="grid grid-cols-4 items-center gap-4">
                        <label htmlFor="teacher" className="text-right text-sm font-medium">
                            Teacher
                        </label>
                        <div className="col-span-3">
                            <select
                                className="flex h-10 w-full rounded-md border border-gray-300 bg-transparent px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                value={selectedTeacherId}
                                onChange={(e) => setSelectedTeacherId(e.target.value)}
                            >
                                <option value="">Select Teacher</option>
                                {teachers.map((teacher) => (
                                    <option key={teacher.id} value={teacher.id}>
                                        {teacher.fullName}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>
                </div>
            </Modal>
        </div>
    );
}
