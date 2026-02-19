"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { api } from "@/lib/api";
import ClassSubjectsManager from "@/components/classes/ClassSubjectsManager";
import { TableSkeleton } from "@/components/ui/Skeleton";

type SchoolClass = {
    id: number;
    name: string;
    section: string;
    stream?: string;
    sessionId: number;
    active: boolean;
};

export default function ClassDetailsPage() {
    const params = useParams();
    const router = useRouter();
    const [schoolClass, setSchoolClass] = useState<SchoolClass | null>(null);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState<"overview" | "subjects">("overview");

    const classId = Number(params.id);

    const loadClass = useCallback(async () => {
        try {
            setLoading(true);
            const res = await api.get(`/api/classes/${classId}`);
            setSchoolClass(res.data);
        } catch {
            setSchoolClass(null); // Keep it null to show error
        } finally {
            setLoading(false);
        }
    }, [classId]);

    useEffect(() => {
        if (classId) {
            void loadClass();
        }
    }, [classId, loadClass]);

    if (loading) {
        return <div className="p-8"><TableSkeleton rows={3} cols={2} /></div>;
    }

    if (!schoolClass && !loading) {
        return (
            <div className="p-8 text-center text-red-500">
                <h3 className="text-lg font-bold">Failed to load class details</h3>
                <p>Class ID: {classId}</p>
                <button
                    onClick={() => router.push("/classes")}
                    className="mt-4 px-4 py-2 bg-gray-100 rounded"
                >
                    Back to List
                </button>
            </div>
        );
    }

    return (
        <div className="space-y-6 p-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">
                        {schoolClass?.name} <span className="text-gray-500 text-xl font-normal">({schoolClass?.section})</span>
                    </h1>
                    <p className="text-gray-500">
                        {schoolClass?.stream ? `${schoolClass.stream} Stream` : "General"} • {schoolClass?.active ? "Active" : "Inactive"}
                    </p>
                </div>
                <button
                    onClick={() => router.push("/classes")}
                    className="px-4 py-2 border rounded-md text-gray-600 hover:bg-gray-50"
                >
                    Back to Classes
                </button>
            </div>

            <div className="border-b border-gray-200">
                <nav className="-mb-px flex space-x-8">
                    <button
                        onClick={() => setActiveTab("overview")}
                        className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm ${activeTab === "overview"
                            ? "border-blue-500 text-blue-600"
                            : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                            }`}
                    >
                        Overview
                    </button>
                    <button
                        onClick={() => setActiveTab("subjects")}
                        className={`whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm ${activeTab === "subjects"
                            ? "border-blue-500 text-blue-600"
                            : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                            }`}
                    >
                        Subjects
                    </button>
                </nav>
            </div>

            <div className="mt-6">
                {activeTab === "overview" && (
                    <div className="bg-white shadow overflow-hidden sm:rounded-lg border">
                        <div className="px-4 py-5 sm:px-6">
                            <h3 className="text-lg leading-6 font-medium text-gray-900">Class Information</h3>
                        </div>
                        <div className="border-t border-gray-200">
                            <dl>
                                <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                                    <dt className="text-sm font-medium text-gray-500">Class Name</dt>
                                    <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{schoolClass?.name}</dd>
                                </div>
                                <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                                    <dt className="text-sm font-medium text-gray-500">Section</dt>
                                    <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{schoolClass?.section}</dd>
                                </div>
                                <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                                    <dt className="text-sm font-medium text-gray-500">Stream</dt>
                                    <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">{schoolClass?.stream || "N/A"}</dd>
                                </div>
                                <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                                    <dt className="text-sm font-medium text-gray-500">Status</dt>
                                    <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                                        <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${schoolClass?.active ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"}`}>
                                            {schoolClass?.active ? "Active" : "Inactive"}
                                        </span>
                                    </dd>
                                </div>
                            </dl>
                        </div>
                    </div>
                )}

                {activeTab === "subjects" && (
                    <div className="bg-white shadow sm:rounded-lg border p-6">
                        <ClassSubjectsManager classId={classId} />
                    </div>
                )}
            </div>
        </div>
    );
}
