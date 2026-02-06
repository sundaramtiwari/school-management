package com.school.backend.common.tenant;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantFilterAspect {

    private final EntityManager entityManager;

    public TenantFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Run after any method that opens a Hibernate Session to ensure the filter is
     * enabled.
     * This targets Service methods or Transactional boundaries.
     */
    @AfterReturning(pointcut = "execution(* com.school.backend..service..*(..))" +
            " || @annotation(org.springframework.transaction.annotation.Transactional)", returning = "result")
    public void enableTenantFilter(Object result) {
        // We actually need this to run BEFORE queries, so @AfterReturning is likely too
        // late for the current transaction
        // if the session was opened just for this method.
        // However, standard practice is to use an Interceptor or run it on Session
        // creation.
        // A better approach for Spring Boot is using AOP 'Before'.
    }

    @org.aspectj.lang.annotation.Before("execution(* com.school.backend..service..*(..))" +
            " || @annotation(org.springframework.transaction.annotation.Transactional)")
    public void enableFilter() {
        Long schoolId = TenantContext.getSchoolId();

        if (schoolId != null) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("schoolId", schoolId);
        }
    }
}
