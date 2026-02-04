package com.school.backend.common.tenant;

public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_SCHOOL = new ThreadLocal<>();

    public static Long getSchoolId() {
        return CURRENT_SCHOOL.get();
    }

    public static void setSchoolId(Long schoolId) {
        CURRENT_SCHOOL.set(schoolId);
    }

    public static void clear() {
        CURRENT_SCHOOL.remove();
    }
}
