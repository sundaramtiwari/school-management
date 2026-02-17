import { api } from "./api";

export const sessionApi = {
    list: () =>
        api.get("/api/academic-sessions"),

    create: (data: any) =>
        api.post("/api/academic-sessions", data),

    update: (id: number, data: any) =>
        api.put(`/api/academic-sessions/${id}`, data),

    setCurrent: (sessionId: number) =>
        api.put(`/api/academic-sessions/${sessionId}/set-current`),
};
