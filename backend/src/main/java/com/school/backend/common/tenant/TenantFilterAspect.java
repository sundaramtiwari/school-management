package com.school.backend.common.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* com.school.backend..service..*(..))" +
            " || @annotation(org.springframework.transaction.annotation.Transactional)")
    public void enableFilter() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }

        Long schoolId = TenantContext.getSchoolId();

        if (schoolId != null) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("schoolId", schoolId);
        }
    }
}
