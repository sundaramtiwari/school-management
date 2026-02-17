"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";

type AcademicSession = {
  id: number;
  name: string;
  schoolId: number;
  active: boolean;
};

type SessionContextType = {
  currentSession: AcademicSession | null;
  sessions: AcademicSession[];
  setCurrentSession: (session: AcademicSession) => void;
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
  const refreshSessions = async () => {
    // STRICT: Only fetch sessions if user belongs to a school
    if (!user || !user.schoolId) {
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
          schoolId: user.schoolId,
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
  };

  // Load sessions when user changes - use user?.userId for stability
  useEffect(() => {
    if (user?.schoolId) {
      refreshSessions();
    } else {
      setSessions([]);
      setCurrentSessionState(null);
      setIsSessionLoading(false);
    }
  }, [user?.userId, user?.schoolId]);

  const setCurrentSession = (session: AcademicSession | null) => {
    // We strictly use backend source of truth, so setting manually might be temporary or for optimistic UI?
    // User wants "Session source of truth must be backend school.currentSessionId".
    // This implies we shouldn't really be manually setting currentSession separate from backend.
    // However, if we switch sessions, we probably call an API to update school.currentSessionId?
    // The instructions don't mention session switching logic updates, just "Fix SessionContext".
    // Assuming setCurrentSession updates local state for now, but maybe it should be removed or strictly strictly coupled?
    // For now, let's keep it but remove localStorage as requested.
    setCurrentSessionState(session);
  };

  // Axios interceptor to inject X-Session-Id header
  useEffect(() => {
    const interceptor = api.interceptors.request.use((config) => {
      if (currentSession) {
        config.headers["X-Session-Id"] = currentSession.id.toString();
      }
      return config;
    });

    return () => {
      api.interceptors.request.eject(interceptor);
    };
  }, [currentSession]);

  // Fetch class count when session changes
  useEffect(() => {
    // Only fetch if session exists AND user is logged in with a school
    if (currentSession && user?.schoolId) {
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
        setCurrentSession, // This function might need to call backend to switch session in future, but out of scope now
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
