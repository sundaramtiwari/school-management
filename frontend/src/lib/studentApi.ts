import { api } from "./api";

export const studentApi = {

    listBySchool: (page = 0, size = 20) =>
        api.get(`/api/students/mine?page=${page}&size=${size}`),

    getById: (id: number) =>
        api.get(`/api/students/${id}`),

    byClass: (classId: number, page = 0, size = 20) =>
        api.get(`/api/students/by-class/${classId}?page=${page}&size=${size}`),

    create: (data: any) =>
        api.post("/api/students", data),

    enroll: (data: any) =>
        api.post("/api/enrollments", data),

    update: (id: number, data: any) =>
        api.put(`/api/students/${id}`, data),

    delete: (id: number) =>
        api.delete(`/api/students/${id}`),

};
