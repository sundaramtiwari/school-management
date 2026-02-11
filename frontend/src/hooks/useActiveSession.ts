"use client";

import { useEffect, useState } from "react";
import { sessionApi } from "@/lib/sessionApi";
import { useAuth } from "@/context/AuthContext";

/**
 * Hook that fetches the current active academic session name.
 * Returns { session, loading } where session is e.g. "2024-25" or "" if none found.
 */
export function useActiveSession() {
    const { user } = useAuth();
    const [session, setSession] = useState("");
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!user?.schoolId) {
            setLoading(false);
            return;
        }
        let cancelled = false;
        (async () => {
            try {
                const res = await sessionApi.list();
                const sessions: { id: number; name: string; isCurrent: boolean }[] = res.data || [];
                const current = sessions.find(s => s.isCurrent);
                if (!cancelled) setSession(current?.name || sessions[0]?.name || "");
            } catch {
                // Fallback silently
            } finally {
                if (!cancelled) setLoading(false);
            }
        })();
        return () => { cancelled = true; };
    }, [user?.schoolId]);

    return { session, loading };
}
