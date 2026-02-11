import { api } from "./api";

export const transportApi = {
    // Routes
    getAllRoutes: () => api.get("/api/transport/routes"),
    createRoute: (data: { name: string; description?: string }) =>
        api.post("/api/transport/routes", data),

    // Pickup Points
    getPickupsByRoute: (routeId: number) =>
        api.get(`/api/transport/pickup-points/by-route/${routeId}`),
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
        session: string;
    }) => api.post("/api/transport/enrollments", data),

    getEnrollmentByStudent: (studentId: number, session: string) =>
        api.get(`/api/transport/enrollments/student/${studentId}?session=${session}`),
};
