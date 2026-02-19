"use client";

import React from "react";

interface StudentItem {
    id: number;
    firstName: string;
    lastName: string;
    admissionNumber?: string;
}

interface SubjectItem {
    id: number;
    subjectId: number;
    subjectName?: string;
    maxMarks: number;
}

interface MarkItem {
    studentId: number;
    examSubjectId: number;
    marksObtained: number;
}

interface MarksheetMVPProps {
    students: StudentItem[];
    subjects: SubjectItem[];
    marks: MarkItem[];
}

export default function MarksheetMVP({ students, subjects, marks }: MarksheetMVPProps) {

    if (subjects.length === 0) {
        return (
            <div className="p-20 text-center text-gray-400 italic">
                Please add subjects to view results.
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h3 className="font-bold text-gray-700">Class Results Preview</h3>
                <p className="text-xs text-gray-400 uppercase font-bold tracking-widest">MVP Marksheet View</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {students.map((st) => {
                    const studentMarks = marks.filter(m => m.studentId === st.id);

                    let totalObtained = 0;
                    let totalMax = 0;

                    return (
                        <div key={st.id} className="bg-white border rounded-2xl shadow-sm overflow-hidden flex flex-col">
                            <div className="p-4 bg-gray-50 border-b">
                                <h4 className="font-bold text-gray-800">{st.firstName} {st.lastName}</h4>
                                <p className="text-[10px] text-gray-400 font-bold uppercase tracking-tighter">Adm No: {st.admissionNumber || "N/A"}</p>
                            </div>

                            <div className="flex-1 p-4">
                                <table className="w-full text-xs">
                                    <thead className="text-gray-400 border-b">
                                        <tr>
                                            <th className="text-left py-2 font-normal">Subject</th>
                                            <th className="text-right py-2 font-normal">Score</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y">
                                        {subjects.map((s) => {
                                            const mark = studentMarks.find(m => m.examSubjectId === s.id);
                                            const obtained = mark?.marksObtained || 0;
                                            totalObtained += obtained;
                                            totalMax += s.maxMarks;

                                            return (
                                                <tr key={s.id}>
                                                    <td className="py-2 text-gray-600 font-medium">{s.subjectName || `Subject #${s.subjectId}`}</td>
                                                    <td className="py-2 text-right font-mono font-bold">
                                                        {obtained} <span className="text-[10px] text-gray-300 font-normal">/ {s.maxMarks}</span>
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>

                            <div className="p-4 bg-blue-50/50 border-t flex justify-between items-center">
                                <div className="space-y-0.5">
                                    <p className="text-[10px] text-gray-400 uppercase font-bold tracking-tighter">Total Marks</p>
                                    <p className="text-sm font-bold text-blue-700 uppercase">
                                        {totalObtained} <span className="text-xs text-blue-400 font-normal">/ {totalMax}</span>
                                    </p>
                                </div>
                                <div className="text-right">
                                    <p className="text-[10px] text-gray-400 uppercase font-bold tracking-tighter">Percentage</p>
                                    <p className="text-xl font-black text-blue-800">
                                        {totalMax > 0 ? ((totalObtained / totalMax) * 100).toFixed(1) : "0"}%
                                    </p>
                                </div>
                            </div>
                        </div>
                    );
                })}
            </div>

            {students.length === 0 && (
                <div className="p-20 text-center text-gray-400 italic">
                    No students found in this class.
                </div>
            )}
        </div>
    );
}
