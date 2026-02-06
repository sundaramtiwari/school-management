import { api } from "./api";

export const sessionApi = {
    list: (schoolId: number) =>
        api.get(`/api/academic-sessions?schoolId=${schoolId}`),

    create: (data: any) =>
        api.post("/api/academic-sessions", data),

    update: (id: number, data: any) =>
        api.put(`/api/academic-sessions/${id}`, data),
};
