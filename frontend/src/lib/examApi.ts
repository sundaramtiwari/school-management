import { api } from "./api";

export const examApi = {
    listByClass: (classId: number, sessionId: number) =>
        api.get(`/api/exams/by-class/${classId}?sessionId=${sessionId}`),

    create: (data: any) =>
        api.post("/api/exams", data),

    listSubjects: (examId: number) =>
        api.get(`/api/exam-subjects/by-exam/${examId}`),

    addSubject: (data: any) =>
        api.post("/api/exam-subjects", data),

    listMarks: (examId: number) =>
        api.get(`/api/marks/exam/${examId}`),

    saveMarksBulk: (examId: number, data: any) =>
        api.post(`/api/exams/${examId}/marks/bulk`, data),
};
