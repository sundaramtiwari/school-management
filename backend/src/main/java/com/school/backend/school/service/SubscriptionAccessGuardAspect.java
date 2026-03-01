package com.school.backend.school.service;

import com.school.backend.common.tenant.TenantContext;
import com.school.backend.common.enums.UserRole;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class SubscriptionAccessGuardAspect {

    private final SubscriptionAccessService accessService;

    @Before(
            "execution(* com.school.backend.core..controller..*(..))" +
            " || execution(* com.school.backend.fee..controller..*(..))" +
            " || execution(* com.school.backend.finance..controller..*(..))" +
            " || execution(* com.school.backend.transport..controller..*(..))" +
            " || execution(* com.school.backend.testmanagement..controller..*(..))")
    public void validateSchoolScopedAccess() {
        Long schoolId = TenantContext.getSchoolId();
        if (schoolId == null) {
            return;
        }
        UserRole role = SecurityUtil.role();
        if (role == UserRole.SUPER_ADMIN || role == UserRole.PLATFORM_ADMIN
                || role == UserRole.SCHOOL_ADMIN || role == UserRole.TEACHER || role == UserRole.ACCOUNTANT) {
            accessService.validateSchoolAccess(schoolId);
        }
    }
}
