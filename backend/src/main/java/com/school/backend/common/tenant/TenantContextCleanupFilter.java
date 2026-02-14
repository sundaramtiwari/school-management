package com.school.backend.common.tenant;

import jakarta.servlet.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter that ensures TenantContext and SessionContext are always cleared after
 * request processing.
 * Runs AFTER all other filters and processing by using a very low order
 * priority.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TenantContextCleanupFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            // Clean up ThreadLocal variables after the entire request is processed
            TenantContext.clear();
            SessionContext.clear();
        }
    }
}
