import { api } from "./api";

export interface SubjectData {
    id?: number;
    name: string;
    code?: string;
    type?: string;
    maxMarks?: number;
    minMarks?: number;
    active?: boolean;
    remarks?: string;
    schoolId?: number;
}

export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
}

export const subjectApi = {
    getAll: async (page = 0, size = 20, includeInactive = false) => {
        const response = await api.get<PageResponse<SubjectData>>(
            `/api/subjects/mine?page=${page}&size=${size}&includeInactive=${includeInactive}`
        );
        return response.data;
    },

    create: async (data: SubjectData) => {
        const response = await api.post<SubjectData>("/api/subjects", data);
        return response.data;
    },

    update: async (id: number, data: SubjectData) => {
        const response = await api.put<SubjectData>(`/api/subjects/${id}`, data);
        return response.data;
    },

    toggle: async (id: number) => {
        const response = await api.patch<SubjectData>(`/api/subjects/${id}/toggle`);
        return response.data;
    },

    delete: async (id: number) => {
        // Historically delete might have been used, but we now prefer toggle for soft-delete
        await api.delete(`/api/subjects/${id}`);
    },
};
