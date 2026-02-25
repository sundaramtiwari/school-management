import axios from "axios";

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
  },
});

// Interceptor to attach Authorization header and X-School-Id from localStorage
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  const schoolId = localStorage.getItem("schoolId");
  if (schoolId) {
    config.headers["X-School-Id"] = schoolId;
  }

  const sessionId = localStorage.getItem("sessionId");
  if (sessionId) {
    config.headers["X-Session-Id"] = sessionId;
  }

  return config;
});

// Response interceptor to handle 401 Unauthorized
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear auth data
      localStorage.removeItem("token");
      localStorage.removeItem("user");

      // Redirect to login if not already there
      if (!window.location.pathname.startsWith("/login")) {
        window.location.href = `/login?expired=true`;
      }
    } else if (error.response?.status === 403) {
      // Special handle for forbidden access to show a better message
      if (error.response.data && !error.response.data.message) {
        error.response.data.message = "Access Denied: You do not have permission to view or perform this action.";
      }
    }
    return Promise.reject(error);
  }
);

