"use client";

import { useEffect } from "react";

export type GuardianFormValue = {
    name: string;
    relation: string;
    contactNumber: string;
    email: string;
    address: string;
    aadharNumber: string;
    occupation: string;
    qualification: string;
    primaryGuardian: boolean;
    whatsappEnabled: boolean;
};

interface GuardianFormSectionProps {
    guardians: GuardianFormValue[];
    onChange: (guardians: GuardianFormValue[]) => void;
}

export default function GuardianFormSection({ guardians, onChange }: GuardianFormSectionProps) {

    // Ensure at least one primary if list not empty
    useEffect(() => {
        if (guardians.length > 0 && !guardians.some(g => g.primaryGuardian)) {
            const newGuardians = [...guardians];
            newGuardians[0].primaryGuardian = true;
            onChange(newGuardians);
        }
    }, [guardians.length]);

    const addGuardian = () => {
        const newGuardian: GuardianFormValue = {
            name: "",
            relation: "FATHER",
            contactNumber: "",
            email: "",
            address: "",
            aadharNumber: "",
            occupation: "",
            qualification: "",
            primaryGuardian: guardians.length === 0,
            whatsappEnabled: true,
        };
        onChange([...guardians, newGuardian]);
    };

    const removeGuardian = (index: number) => {
        const newGuardians = guardians.filter((_, i) => i !== index);
        onChange(newGuardians);
    };

    const updateGuardian = (index: number, field: keyof GuardianFormValue, value: string | boolean) => {
        const newGuardians = [...guardians];

        if (field === "primaryGuardian" && value === true) {
            // Only one primary allowed
            newGuardians.forEach((g, i) => {
                g.primaryGuardian = i === index;
            });
        } else {
            newGuardians[index] = { ...newGuardians[index], [field]: value };
        }

        onChange(newGuardians);
    };

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold text-gray-900">Guardians</h3>
                <button
                    type="button"
                    onClick={addGuardian}
                    className="inline-flex items-center gap-2 px-3 py-1.5 rounded-md border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                    <span aria-hidden="true">+</span> Add Guardian
                </button>
            </div>

            {guardians.length === 0 && (
                <div className="text-center py-6 border-2 border-dashed border-gray-200 rounded-lg text-gray-500">
                    At least one guardian is required.
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {guardians.map((guardian, index) => (
                    <div key={index} className="relative p-4 border rounded-lg bg-white shadow-sm space-y-3">
                        <div className="flex items-center justify-between">
                            <span className="text-sm font-medium text-gray-500"># {index + 1}</span>
                            <div className="flex items-center gap-2">
                                <button
                                    type="button"
                                    onClick={() => updateGuardian(index, "primaryGuardian", true)}
                                    className={`flex items-center gap-1 px-2 py-1 rounded text-xs transition-colors ${guardian.primaryGuardian
                                            ? "bg-green-100 text-green-700 font-bold"
                                            : "bg-gray-100 text-gray-500 hover:bg-gray-200"
                                        }`}
                                >
                                    <span aria-hidden="true">✓</span>
                                    Primary
                                </button>
                                <button
                                    type="button"
                                    onClick={() => removeGuardian(index)}
                                    className="text-red-500 hover:text-red-700 transition-colors"
                                >
                                    <span aria-hidden="true">🗑</span>
                                </button>
                            </div>
                        </div>

                        <div className="space-y-3">
                            <div className="grid grid-cols-2 gap-3">
                                <div className="space-y-1">
                                    <label className="text-xs font-semibold text-gray-600">Name *</label>
                                    <input
                                        type="text"
                                        required
                                        value={guardian.name}
                                        onChange={(e) => updateGuardian(index, "name", e.target.value)}
                                        className="w-full text-sm p-2 border rounded focus:ring-1 focus:ring-blue-500 outline-none"
                                        placeholder="Full Name"
                                    />
                                </div>
                                <div className="space-y-1">
                                    <label className="text-xs font-semibold text-gray-600">Relation *</label>
                                    <select
                                        value={guardian.relation}
                                        onChange={(e) => updateGuardian(index, "relation", e.target.value)}
                                        className="w-full text-sm p-2 border rounded focus:ring-1 focus:ring-blue-500 outline-none"
                                    >
                                        <option value="FATHER">Father</option>
                                        <option value="MOTHER">Mother</option>
                                        <option value="GUARDIAN">Guardian</option>
                                        <option value="OTHER">Other</option>
                                    </select>
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-3">
                                <div className="space-y-1">
                                    <label className="text-xs font-semibold text-gray-600">Contact Number *</label>
                                    <input
                                        type="tel"
                                        required
                                        value={guardian.contactNumber}
                                        onChange={(e) => updateGuardian(index, "contactNumber", e.target.value)}
                                        className="w-full text-sm p-2 border rounded focus:ring-1 focus:ring-blue-500 outline-none"
                                        placeholder="10-digit mobile"
                                    />
                                </div>
                                <div className="space-y-1">
                                    <label className="text-xs font-semibold text-gray-600">WhatsApp Alert</label>
                                    <button
                                        type="button"
                                        onClick={() => updateGuardian(index, "whatsappEnabled", !guardian.whatsappEnabled)}
                                        className={`flex items-center gap-2 w-full p-2 border rounded text-sm transition-colors ${guardian.whatsappEnabled
                                                ? "bg-green-50 border-green-200 text-green-700"
                                                : "bg-white border-gray-200 text-gray-500"
                                            }`}
                                    >
                                        <span aria-hidden="true" className={guardian.whatsappEnabled ? "text-green-600" : "text-gray-400"}>💬</span>
                                        {guardian.whatsappEnabled ? "Enabled" : "Enable"}
                                    </button>
                                </div>
                            </div>

                            <div className="space-y-1">
                                <label className="text-xs font-semibold text-gray-600">Aadhar Number</label>
                                <input
                                    type="text"
                                    value={guardian.aadharNumber}
                                    onChange={(e) => updateGuardian(index, "aadharNumber", e.target.value)}
                                    className="w-full text-sm p-2 border rounded focus:ring-1 focus:ring-blue-500 outline-none"
                                    placeholder="12-digit Aadhar"
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-3">
                                <div className="space-y-1">
                                    <label className="text-xs font-semibold text-gray-600">Occupation</label>
                                    <input
                                        type="text"
                                        value={guardian.occupation}
                                        onChange={(e) => updateGuardian(index, "occupation", e.target.value)}
                                        className="w-full text-sm p-2 border rounded focus:ring-1 focus:ring-blue-500 outline-none"
                                        placeholder="e.g. Business"
                                    />
                                </div>
                                <div className="space-y-1">
                                    <label className="text-xs font-semibold text-gray-600">Qualification</label>
                                    <input
                                        type="text"
                                        value={guardian.qualification}
                                        onChange={(e) => updateGuardian(index, "qualification", e.target.value)}
                                        className="w-full text-sm p-2 border rounded focus:ring-1 focus:ring-blue-500 outline-none"
                                        placeholder="e.g. Graduate"
                                    />
                                </div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
