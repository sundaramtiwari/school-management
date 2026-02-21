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
      const [sessionsRes, activeRes] = await Promise.all([
        api.get<AcademicSession[]>("/api/academic-sessions"),
        api.get<{ exists: boolean; sessionId: number; name: string }>("/api/academic-sessions/active")
      ]);

      const fetchedSessions = sessionsRes.data;
      const activeInfo = activeRes.data;
      setSessions(fetchedSessions);

      // STRICT: Determine currentSession using activeInfo
      if (activeInfo.exists && activeInfo.sessionId) {
        const activeSession = fetchedSessions.find(s => s.id === activeInfo.sessionId);
        // Fallback to minimal object if not found in list (though it should be)
        setCurrentSessionState(activeSession || {
          id: activeInfo.sessionId,
          name: activeInfo.name,
          schoolId: (user.schoolId ?? Number(selectedSchoolId)) as number,
          startDate: "",
          endDate: "",
          active: true
        });
      } else {
        setCurrentSessionState(null);
      }

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

  function isPlatformRole(role?: string) {
    return ["SUPER_ADMIN", "PLATFORM_ADMIN"].includes(role?.toUpperCase() || "");
  }

  // Axios interceptor to inject X-Session-Id header
  useEffect(() => {
    if (currentSession) {
      localStorage.setItem("sessionId", currentSession.id.toString());
    } else {
      localStorage.removeItem("sessionId");
    }

    const interceptor = api.interceptors.request.use((config) => {
      if (currentSession) {
        config.headers["X-Session-Id"] = currentSession.id.toString();
      }
      return config;
    });

    return () => {
      api.interceptors.request.eject(interceptor);
    };
  }, [currentSession?.id]);

  // Fetch class count when session changes
  useEffect(() => {
    // Ensure classes count respects selected school context
    const selectedSchoolId = localStorage.getItem("schoolId");
    if (currentSession && (user?.schoolId || selectedSchoolId)) {
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
  }, [currentSession, user?.schoolId]);

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
