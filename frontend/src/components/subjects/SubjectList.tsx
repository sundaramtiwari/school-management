"use client";

import { useCallback, useEffect, useState } from "react";
import { subjectApi, SubjectData } from "@/lib/subjectApi";
import { SubjectModal } from "./SubjectModal";
import { useAuth } from "@/context/AuthContext";

export default function SubjectList() {
    const { user } = useAuth();
    const [subjects, setSubjects] = useState<SubjectData[]>([]);
    const [loading, setLoading] = useState(true);
    const [showInactive, setShowInactive] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedSubject, setSelectedSubject] = useState<SubjectData | null>(null);

    const fetchSubjects = useCallback(async () => {
        setLoading(true);
        try {
            // If showInactive is true -> We want ALL subjects -> active=undefined
            // If showInactive is false -> We want ACTIVE ONLY -> active=true
            const activeParam = showInactive ? undefined : true;
            const data = await subjectApi.getAll(page, 20, activeParam);
            setSubjects(data.content);
            setTotalPages(data.totalPages);
        } catch (error) {
            console.error("Failed to fetch subjects", error);
        } finally {
            setLoading(false);
        }
    }, [page, showInactive]);

    useEffect(() => {
        fetchSubjects();
    }, [fetchSubjects]);

    const handleEdit = (subject: SubjectData) => {
        setSelectedSubject(subject);
        setIsModalOpen(true);
    };

    const handleCreate = () => {
        setSelectedSubject(null);
        setIsModalOpen(true);
    };

    const handleSuccess = () => {
        fetchSubjects();
    };

    return (
        <div className="mx-auto px-6 py-6">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-lg font-semibold">Subject Master</h1>
                <div className="flex items-center gap-4">
                    <div className="flex items-center space-x-2">
                        <input
                            type="checkbox"
                            id="showInactive"
                            checked={showInactive}
                            onChange={(e) => setShowInactive(e.target.checked)}
                            className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                        />
                        <label htmlFor="showInactive" className="text-sm font-medium leading-none text-gray-700">
                            Show Inactive
                        </label>
                    </div>
                    {/* Expand FE gating to match backend permissions */}
                    {["SCHOOL_ADMIN", "SUPER_ADMIN", "PLATFORM_ADMIN"].includes(user?.role?.toUpperCase() ?? "") && (
                        <button
                            onClick={handleCreate}
                            className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
                        >
                            + Add Subject
                        </button>
                    )}
                </div>
            </div>

            <div className="bg-white rounded-lg shadow border border-gray-100 overflow-hidden mt-4">
                <table className="w-full text-base text-left">
                    <thead className="bg-gray-50 text-gray-600 text-lg font-semibold border-b border-gray-100">
                        <tr>
                            <th className="px-6 py-3">Name</th>
                            <th className="px-6 py-3">Code</th>
                            <th className="px-6 py-3">Type</th>
                            <th className="px-6 py-3">Marks (Min/Max)</th>
                            <th className="px-6 py-3">Status</th>
                            <th className="px-6 py-3">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr>
                                <td colSpan={6} className="px-6 py-4 text-center">Loading...</td>
                            </tr>
                        ) : subjects.length === 0 ? (
                            <tr>
                                <td colSpan={6} className="px-6 py-4 text-center">No subjects found.</td>
                            </tr>
                        ) : (
                            subjects.map((subject) => (
                                <tr key={subject.id} className="border-b hover:bg-gray-50 transition-colors">
                                    <td className="px-6 py-4 font-medium">{subject.name}</td>
                                    <td className="px-6 py-4">{subject.code || "-"}</td>
                                    <td className="px-6 py-4">{subject.type}</td>
                                    <td className="px-6 py-4">{subject.minMarks} / {subject.maxMarks}</td>
                                    <td className="px-6 py-4">
                                        <span className={`px-2 py-1 rounded text-xs font-semibold ${subject.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                                            {subject.active ? "Active" : "Inactive"}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4">
                                        {/* Expand FE gating to match backend permissions */}
                                        {["SCHOOL_ADMIN", "SUPER_ADMIN", "PLATFORM_ADMIN"].includes(user?.role?.toUpperCase() ?? "") && (
                                            <button
                                                onClick={() => handleEdit(subject)}
                                                className="text-blue-600 hover:text-blue-800 font-medium bg-blue-50 px-3 py-1 rounded hover:bg-blue-100 transition-colors"
                                            >
                                                Edit
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            <div className="mt-4 flex justify-between items-center">
                <button
                    disabled={page === 0}
                    onClick={() => setPage(page - 1)}
                    className="px-4 py-2 rounded-md bg-white border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:opacity-50"
                >
                    Previous
                </button>
                <span className="text-base text-gray-500">Page {page + 1} of {totalPages === 0 ? 1 : totalPages}</span>
                <button
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage(page + 1)}
                    className="px-4 py-2 rounded-md bg-white border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:opacity-50"
                >
                    Next
                </button>
            </div>

            <SubjectModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                initialData={selectedSubject}
                onSuccess={handleSuccess}
            />
        </div>
    );
}
