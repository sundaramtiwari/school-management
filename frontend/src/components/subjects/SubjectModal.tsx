"use client";

import { useEffect, useState } from "react";
import Modal from "@/components/ui/Modal";
import { SubjectData, subjectApi } from "@/lib/subjectApi";

interface SubjectModalProps {
    isOpen: boolean;
    onClose: () => void;
    initialData?: SubjectData | null;
    onSuccess: () => void;
}

export function SubjectModal({ isOpen, onClose, initialData, onSuccess }: SubjectModalProps) {
    const [formData, setFormData] = useState<SubjectData>({
        name: "",
        code: "",
        type: "THEORY",
        maxMarks: 100,
        minMarks: 35,
        active: true,
        remarks: "",
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        if (initialData) {
            setFormData(initialData);
        } else {
            setFormData({
                name: "",
                code: "",
                type: "THEORY",
                maxMarks: 100,
                minMarks: 35,
                active: true,
                remarks: "",
            });
        }
        setError("");
    }, [initialData, isOpen]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError("");

        try {
            if (initialData?.id) {
                await subjectApi.update(initialData.id, formData);
            } else {
                await subjectApi.create(formData);
            }
            onSuccess();
            onClose();
        } catch (err: any) {
            console.error(err);
            setError(err.response?.data?.message || "Failed to save subject");
        } finally {
            setLoading(false);
        }
    };

    const footer = (
        <div className="flex justify-end gap-2">
            <button
                type="button"
                onClick={onClose}
                disabled={loading}
                className="px-4 py-2 border rounded-md text-gray-600 hover:bg-gray-50"
            >
                Cancel
            </button>
            <button
                onClick={handleSubmit}
                disabled={loading}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
            >
                {loading ? "Saving..." : "Save"}
            </button>
        </div>
    );

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title={initialData ? "Edit Subject" : "New Subject"}
            footer={footer}
        >
            <form onSubmit={handleSubmit} className="space-y-4">
                {error && <div className="text-red-500 text-sm">{error}</div>}

                <div className="grid grid-cols-4 items-center gap-4">
                    <label htmlFor="name" className="text-right font-medium text-sm">
                        Name
                    </label>
                    <input
                        id="name"
                        value={formData.name}
                        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                        className="col-span-3 flex h-10 w-full rounded-md border border-gray-300 bg-transparent px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                    />
                </div>

                <div className="grid grid-cols-4 items-center gap-4">
                    <label htmlFor="code" className="text-right font-medium text-sm">
                        Code
                    </label>
                    <input
                        id="code"
                        value={formData.code || ""}
                        onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                        className="col-span-3 flex h-10 w-full rounded-md border border-gray-300 bg-transparent px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>

                <div className="grid grid-cols-4 items-center gap-4">
                    <label htmlFor="type" className="text-right font-medium text-sm">
                        Type
                    </label>
                    <select
                        id="type"
                        className="col-span-3 flex h-10 w-full rounded-md border border-gray-300 bg-transparent px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                        value={formData.type}
                        onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                    >
                        <option value="THEORY">Theory</option>
                        <option value="PRACTICAL">Practical</option>
                        <option value="LANGUAGE">Language</option>
                    </select>
                </div>

                <div className="grid grid-cols-4 items-center gap-4">
                    <label htmlFor="maxMarks" className="text-right font-medium text-sm">
                        Max Marks
                    </label>
                    <input
                        id="maxMarks"
                        type="number"
                        value={formData.maxMarks}
                        onChange={(e) => setFormData({ ...formData, maxMarks: parseInt(e.target.value) })}
                        className="col-span-3 flex h-10 w-full rounded-md border border-gray-300 bg-transparent px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>

                <div className="grid grid-cols-4 items-center gap-4">
                    <label htmlFor="minMarks" className="text-right font-medium text-sm">
                        Min Marks
                    </label>
                    <input
                        id="minMarks"
                        type="number"
                        value={formData.minMarks}
                        onChange={(e) => setFormData({ ...formData, minMarks: parseInt(e.target.value) })}
                        className="col-span-3 flex h-10 w-full rounded-md border border-gray-300 bg-transparent px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>

                <div className="grid grid-cols-4 items-center gap-4">
                    <label htmlFor="active" className="text-right font-medium text-sm">
                        Active
                    </label>
                    <div className="col-span-3 flex items-center space-x-2">
                        <input
                            type="checkbox"
                            id="active"
                            checked={formData.active}
                            onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                            className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                        />
                        <label htmlFor="active" className="text-sm font-medium leading-none text-gray-700">
                            Is Active?
                        </label>
                    </div>
                </div>

                <div className="grid grid-cols-4 items-center gap-4">
                    <label htmlFor="remarks" className="text-right font-medium text-sm">
                        Remarks
                    </label>
                    <input
                        id="remarks"
                        value={formData.remarks || ""}
                        onChange={(e) => setFormData({ ...formData, remarks: e.target.value })}
                        className="col-span-3 flex h-10 w-full rounded-md border border-gray-300 bg-transparent px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>
            </form>
        </Modal>
    );
}
