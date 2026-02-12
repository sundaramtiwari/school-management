"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { api } from "@/lib/api";

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
  isLoading: boolean;
};

const SessionContext = createContext<SessionContextType | null>(null);

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [currentSession, setCurrentSessionState] = useState<AcademicSession | null>(null);
  const [sessions, setSessions] = useState<AcademicSession[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Load from localStorage on mount
  useEffect(() => {
    const storedSession = localStorage.getItem("currentSession");
    if (storedSession) {
      try {
        setCurrentSessionState(JSON.parse(storedSession));
      } catch (e) {
        console.error("Failed to parse stored session", e);
      }
    }
    setIsLoading(false);
  }, []);

  // Fetch sessions from API
  const refreshSessions = async () => {
    try {
      const response = await api.get<AcademicSession[]>("/api/academic-sessions");
      setSessions(response.data);
      
      // If no current session is set, set the first active session
      if (!currentSession && response.data.length > 0) {
        const firstActive = response.data.find(s => s.active);
        if (firstActive) {
          setCurrentSession(firstActive);
        }
      }
    } catch (error) {
      console.error("Failed to fetch sessions", error);
    }
  };

  // Load sessions on mount
  useEffect(() => {
    refreshSessions();
  }, []);

  const setCurrentSession = (session: AcademicSession) => {
    localStorage.setItem("currentSession", JSON.stringify(session));
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

  return (
    <SessionContext.Provider
      value={{
        currentSession,
        sessions,
        setCurrentSession,
        refreshSessions,
        isLoading,
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
