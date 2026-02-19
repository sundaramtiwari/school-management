"use client";

import { useCallback, useEffect, useState } from "react";
import { sessionApi } from "@/lib/sessionApi";
import { useAuth } from "@/context/AuthContext";

type Session = {
    id: number;
    name: string;
    isCurrent: boolean;
};

type Props = {
    value: string;
    onChange: (val: string) => void;
    className?: string;
    placeholder?: string;
};

export default function SessionSelect({ value, onChange, className, placeholder = "Select Session" }: Props) {
    const { user } = useAuth();
    const [sessions, setSessions] = useState<Session[]>([]);
    const [loading, setLoading] = useState(false);

    const loadSessions = useCallback(async () => {
        try {
            setLoading(true);
            const res = await sessionApi.list();
            setSessions(res.data);

            // Auto-select current session if value is empty
            if (!value) {
                const current = res.data.find((s: Session) => s.isCurrent);
                if (current) onChange(current.name);
            }
        } catch (e) {
            console.error("Failed to load sessions", e);
        } finally {
            setLoading(false);
        }
    }, [onChange, value]);

    useEffect(() => {
        if (user?.schoolId) {
            loadSessions();
        }
    }, [user?.schoolId, loadSessions]);

    return (
        <select
            value={value}
            onChange={(e) => onChange(e.target.value)}
            className={className || "border p-2 rounded w-full"}
            disabled={loading}
        >
            <option value="">{placeholder}</option>
            {sessions.map((s) => (
                <option key={s.id} value={s.name}>
                    {s.name} {s.isCurrent ? "(Current)" : ""}
                </option>
            ))}
        </select>
    );
}
