package com.school.backend.user.security;

import com.school.backend.common.enums.UserRole;
import com.school.backend.common.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static CustomUserDetails current() {

        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();

        return (CustomUserDetails) auth.getPrincipal();
    }

    public static Long schoolId() {
        // Return from TenantContext, not from User entity
        return TenantContext.getSchoolId();
    }

    public static UserRole role() {
        return current().getRole();
    }
}
