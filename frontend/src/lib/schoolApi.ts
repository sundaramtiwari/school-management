import { api } from "./api";

export const schoolApi = {
  list: (page = 0, size = 10) =>
    api.get(`/schools?page=${page}&size=${size}`),

  create: (data: any) =>
    api.post("/schools", data),

  delete: (id: number) =>
    api.delete(`/schools/${id}`),
};
