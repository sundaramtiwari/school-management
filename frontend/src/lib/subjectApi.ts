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
    getAll: async (page = 0, size = 20, active?: boolean) => {
        let url = `/api/subjects/mine?page=${page}&size=${size}`;
        if (active !== undefined) {
            url += `&active=${active}`;
        }
        const response = await api.get<PageResponse<SubjectData>>(url);
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

    delete: async (id: number) => {
        await api.delete(`/api/subjects/${id}`);
    },
};
