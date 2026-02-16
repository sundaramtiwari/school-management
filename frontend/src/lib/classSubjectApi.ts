import { api } from "./api";
import { SubjectData } from "./subjectApi";

export interface ClassSubjectData {
    id: number;
    classId: number;
    subjectId: number;
    subjectName: string; // Flattened for display
    subjectCode: string;
    teacherId?: number;
    teacherName?: string;
}

export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
}

export const classSubjectApi = {
    getByClass: async (classId: number, page = 0, size = 20) => {
        const response = await api.get<PageResponse<ClassSubjectData>>(`/api/class-subjects/by-class/${classId}?page=${page}&size=${size}`);
        return response.data;
    },

    assign: async (classId: number, subjectId: number, teacherId?: number) => {
        const response = await api.post<ClassSubjectData>("/api/class-subjects", { classId, subjectId, teacherId });
        return response.data;
    },

    remove: async (id: number) => {
        await api.delete(`/api/class-subjects/${id}`);
    },
};
