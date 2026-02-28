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

/**
 * Extracts a user-friendly error message from an API error.
 *
 * - For 4xx errors: returns the backend's `message` field, which contains
 *   the business-level reason (e.g. "All exams must be LOCKED before promotion").
 * - For 5xx / network errors: returns the provided `fallback` string so that
 *   internal details never leak to the UI.
 *
 * Usage:
 *   } catch (err) {
 *     showToast(extractApiError(err, "Failed to promote students"), "error");
 *   }
 */
export function extractApiError(error: unknown, fallback: string): string {
  if (
    error &&
    typeof error === "object" &&
    "response" in error
  ) {
    const axiosError = error as { response?: { status?: number; data?: { message?: string } } };
    const status = axiosError.response?.status ?? 0;
    const message = axiosError.response?.data?.message;

    // Only surface backend messages for client errors (4xx).
    // 5xx responses may carry internal details — use the fallback instead.
    if (status >= 400 && status < 500 && message && typeof message === "string") {
      return message;
    }
  }
  return fallback;
}


