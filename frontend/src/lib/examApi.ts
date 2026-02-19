import { api } from "./api";

type ExamPayload = Record<string, unknown>;
type ExamSubjectPayload = Record<string, unknown>;
type MarksPayload = Record<string, unknown>;

export const examApi = {
    listByClass: (classId: number, sessionId: number) =>
        api.get(`/api/exams/by-class/${classId}?sessionId=${sessionId}`),

    create: (data: ExamPayload) =>
        api.post("/api/exams", data),

    listSubjects: (examId: number) =>
        api.get(`/api/exam-subjects/by-exam/${examId}`),

    addSubject: (data: ExamSubjectPayload) =>
        api.post("/api/exam-subjects", data),

    listMarks: (examId: number) =>
        api.get(`/api/marks/exam/${examId}`),

    saveMarksBulk: (examId: number, data: MarksPayload) =>
        api.post(`/api/exams/${examId}/marks/bulk`, data),

    publish: (examId: number) =>
        api.put(`/api/exams/${examId}/publish`),

    lock: (examId: number) =>
        api.put(`/api/exams/${examId}/lock`),
};
