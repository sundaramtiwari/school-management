import { api } from "./api";

type SessionPayload = Record<string, unknown>;

export const sessionApi = {
    list: () =>
        api.get("/api/academic-sessions"),

    create: (data: SessionPayload) =>
        api.post("/api/academic-sessions", data),

    update: (id: number, data: SessionPayload) =>
        api.put(`/api/academic-sessions/${id}`, data),

    setCurrent: (sessionId: number) =>
        api.put(`/api/academic-sessions/${sessionId}/set-current`),
};
