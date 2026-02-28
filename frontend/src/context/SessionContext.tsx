"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";

type AcademicSession = {
  id: number;
  name: string;
  schoolId: number;
  startDate: string;
  endDate: string;
  active: boolean;
};

type SessionContextType = {
  currentSession: AcademicSession | null;
  sessions: AcademicSession[];
  refreshSessions: () => Promise<void>;
  isSessionLoading: boolean;
  hasClasses: boolean;
  hasSession: boolean;
};

const SessionContext = createContext<SessionContextType | null>(null);

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [currentSession, setCurrentSessionState] = useState<AcademicSession | null>(null);
  const [sessions, setSessions] = useState<AcademicSession[]>([]);
  const [isSessionLoading, setIsSessionLoading] = useState(true);
  const [hasClasses, setHasClasses] = useState(false);
  const currentSessionId = currentSession?.id ?? null;

  // Fetch sessions from API
  const refreshSessions = useCallback(async () => {
    // Fix Platform Session Bootstrap: Respect selected school for platform roles
    const selectedSchoolId = localStorage.getItem("schoolId");
    if (!user) return;
    if (!user.schoolId && !selectedSchoolId) {
      setSessions([]);
      setCurrentSessionState(null);
      setIsSessionLoading(false);
      return;
    }

    try {
      setIsSessionLoading(true);
      const sessionsRes = await api.get<AcademicSession[]>("/api/academic-sessions");

      const fetchedSessions = sessionsRes.data;
      setSessions(fetchedSessions);

      // 1. Check if user has a preference in localStorage
      const preferredSessionId = localStorage.getItem("sessionId");

      let selected: AcademicSession | null = null;
      if (preferredSessionId) {
        selected = fetchedSessions.find(s => s.id.toString() === preferredSessionId) || null;
      }

      // 2. Fallback to the global active session if no preference or preferred not found
      if (!selected) {
        selected = fetchedSessions.find(s => s.active) || null;
      }

      setCurrentSessionState(selected);

    } catch (error) {
      console.error("Failed to fetch sessions", error);
      // On error, safest to set to null
      setCurrentSessionState(null);
    } finally {
      setIsSessionLoading(false);
    }
  }, [user]);

  // Load sessions when user changes or selected school changes
  useEffect(() => {
    const selectedSchoolId = localStorage.getItem("schoolId");
    if (user?.schoolId || (isPlatformRole(user?.role) && selectedSchoolId)) {
      refreshSessions();
    } else {
      setSessions([]);
      setCurrentSessionState(null);
      setIsSessionLoading(false);
    }
  }, [refreshSessions, user?.role, user?.schoolId, user?.userId]);

  // Listen for schoolId changes in localStorage (handles stale session)
  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === "schoolId") {
        refreshSessions();
      }
    };

    window.addEventListener("storage", handleStorageChange);

    // Support same-tab updates (e.g. from School Selector)
    const handleLocalSchoolIdChange = () => {
      refreshSessions();
    };
    window.addEventListener("local-storage-schoolId", handleLocalSchoolIdChange);

    // Monkey-patch setItem to fire same-tab event for schoolId
    const originalSetItem = window.localStorage.setItem;
    window.localStorage.setItem = function (key, value) {
      originalSetItem.call(this, key, value);
      if (key === "schoolId") {
        window.dispatchEvent(new Event("local-storage-schoolId"));
      }
    };

    return () => {
      window.removeEventListener("storage", handleStorageChange);
      window.removeEventListener("local-storage-schoolId", handleLocalSchoolIdChange);
      window.localStorage.setItem = originalSetItem;
    };
  }, [refreshSessions]);

  function isPlatformRole(role?: string) {
    return ["SUPER_ADMIN", "PLATFORM_ADMIN"].includes(role?.toUpperCase() || "");
  }

  // Axios interceptor to inject X-Session-Id header
  useEffect(() => {
    const interceptor = api.interceptors.request.use((config) => {
      // Prioritize the actual state (per-user)
      if (currentSessionId) {
        config.headers["X-Session-Id"] = currentSessionId.toString();
      }
      return config;
    });

    return () => {
      api.interceptors.request.eject(interceptor);
    };
  }, [currentSessionId]);

  // Fetch class count when session changes
  useEffect(() => {
    // Ensure classes count respects selected school context
    const selectedSchoolId = localStorage.getItem("schoolId");
    if (currentSessionId && (user?.schoolId || selectedSchoolId)) {
      api.get<{ count: number }>("/api/classes/count")
        .then(res => {
          setHasClasses(res.data.count > 0);
        })
        .catch(err => {
          console.error("Failed to fetch class count", err);
          setHasClasses(false);
        });
    } else {
      setHasClasses(false);
    }
  }, [currentSessionId, user?.schoolId]);

  return (
    <SessionContext.Provider
      value={{
        currentSession,
        sessions,
        refreshSessions,
        isSessionLoading,
        hasClasses,
        hasSession: !!currentSession,
      }}
    >
      {children}
    </SessionContext.Provider>
  );
}

export function useSession() {
  const context = useContext(SessionContext);
  if (!context) {
    throw new Error("useSession must be used within a SessionProvider");
  }
  return context;
}
