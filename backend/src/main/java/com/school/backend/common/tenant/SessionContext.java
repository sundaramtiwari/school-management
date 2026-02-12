package com.school.backend.common.tenant;

public class SessionContext {

    private static final ThreadLocal<Long> CURRENT_SESSION = new ThreadLocal<>();

    public static Long getSessionId() {
        return CURRENT_SESSION.get();
    }

    public static void setSessionId(Long sessionId) {
        CURRENT_SESSION.set(sessionId);
    }

    public static void clear() {
        CURRENT_SESSION.remove();
    }
}
