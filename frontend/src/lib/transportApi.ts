import { api } from "./api";

export const transportApi = {
    // Routes
    getAllRoutes: () => api.get("/api/transport/routes"),
    createRoute: (data: { name: string; description?: string; capacity?: number }) =>
        api.post("/api/transport/routes", data),
    deleteRoute: (id: number) => api.delete(`/api/transport/routes/${id}`),

    // Pickup Points
    getPickupsByRoute: (routeId: number) =>
        api.get(`/api/transport/pickup-points/route/${routeId}`),
    createPickup: (data: {
        name: string;
        amount: number;
        frequency: string;
        routeId: number;
    }) => api.post("/api/transport/pickup-points", data),

    // Enrollment (for later)
    enroll: (data: {
        studentId: number;
        pickupPointId: number;
        sessionId: number;
    }) => api.post("/api/transport/enrollments", data),

    getEnrollmentByStudent: (studentId: number, sessionId: number) =>
        api.get(`/api/transport/enrollments/student/${studentId}?sessionId=${sessionId}`),
};
