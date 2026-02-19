import { api } from "./api";

type StudentPayload = Record<string, unknown>;
type EnrollmentPayload = Record<string, unknown>;

export const studentApi = {

    listBySchool: (page = 0, size = 20) =>
        api.get(`/api/students/mine?page=${page}&size=${size}`),

    getById: (id: number) =>
        api.get(`/api/students/${id}`),

    byClass: (classId: number, page = 0, size = 20) =>
        api.get(`/api/students/by-class/${classId}?page=${page}&size=${size}`),

    create: (data: StudentPayload) =>
        api.post("/api/students", data),

    enroll: (data: EnrollmentPayload) =>
        api.post("/api/enrollments", data),

    update: (id: number, data: StudentPayload) =>
        api.put(`/api/students/${id}`, data),

    delete: (id: number) =>
        api.delete(`/api/students/${id}`),

};
