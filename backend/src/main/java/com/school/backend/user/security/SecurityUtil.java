package com.school.backend.user.security;

import com.school.backend.common.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static CustomUserDetails current() {

        Authentication auth =
            SecurityContextHolder.getContext()
                .getAuthentication();

        return (CustomUserDetails) auth.getPrincipal();
    }

    public static Long schoolId() {
        return current().getSchoolId();
    }

    public static UserRole role() {
        return current().getRole();
    }
}
