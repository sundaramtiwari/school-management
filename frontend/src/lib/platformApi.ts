import axios from "axios";

/**
 * Platform-level API client.
 *
 * RULES:
 *  - Never sends X-School-Id
 *  - Never sends X-Session-Id
 *  - Used exclusively for /platform/* routes
 *  - Auth token still injected (admin must be authenticated)
 */
export const platformApi = axios.create({
    baseURL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
    headers: {
        "Content-Type": "application/json",
    },
});

platformApi.interceptors.request.use((config) => {
    const token = localStorage.getItem("token");
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    // Intentionally NO X-School-Id
    // Intentionally NO X-Session-Id
    return config;
});

platformApi.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            if (!window.location.pathname.startsWith("/login")) {
                window.location.href = `/login?expired=true`;
            }
        }
        return Promise.reject(error);
    }
);
