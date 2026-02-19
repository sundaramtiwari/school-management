import { api } from "./api";

type SchoolPayload = Record<string, unknown>;

export const schoolApi = {

    list: (page = 0, size = 10) =>
        api.get(`/api/schools?page=${page}&size=${size}`),

    getByCode: (code: string) =>
        api.get(`/api/schools/${code}`),

    create: (data: SchoolPayload) =>
        api.post("/api/schools", data),

    update: (schoolCode: string, data: SchoolPayload) =>
        api.patch(`/api/schools/${schoolCode}`, data),

    delete: (id: number) =>
        api.delete(`/api/schools/${id}`),

    onboard: (data: SchoolPayload) =>
        api.post("/api/schools/onboard", data),

};
