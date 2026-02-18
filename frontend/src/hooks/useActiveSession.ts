"use client";

import { useSession } from "@/context/SessionContext";

/**
 * Hook that fetches the current active academic session name.
 * Returns { session, loading } where session is e.g. "2024-25" or "" if none found.
 */
export function useActiveSession() {
    const { currentSession, isSessionLoading } = useSession();
    return { session: currentSession?.name || "", loading: isSessionLoading };
}
