package com.school.backend.user.security;

import com.school.backend.common.tenant.TenantContext;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {

                String token = authHeader.substring(7);

                Claims claims = jwtUtil.parse(token);

                Long schoolId = claims.get("schoolId", Long.class);

                if (schoolId != null) {
                    TenantContext.setSchoolId(schoolId);
                }

                String schoolIdHeader = request.getHeader("X-School-Id");
                String role = claims.get("role", String.class);
                if (schoolIdHeader != null && !schoolIdHeader.isBlank() &&
                        ("SUPER_ADMIN".equals(role) || "PLATFORM_ADMIN".equals(role))) {
                    try {
                        TenantContext.setSchoolId(Long.valueOf(schoolIdHeader));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid X-School-Id header: " + schoolIdHeader);
                    }
                }

                String sessionHeader = request.getHeader("X-Session-Id");
                if (sessionHeader != null && !sessionHeader.isBlank()) {
                    try {
                        SessionContext.setSessionId(Long.valueOf(sessionHeader));
                    } catch (NumberFormatException e) {
                        // Log and ignore invalid session header to prevent request failure
                        logger.warn("Invalid X-Session-Id header: " + sessionHeader);
                    }
                }

                String email = claims.getSubject();

                User user = userRepository
                        .findByEmailAndActiveTrue(email)
                        .orElse(null);

                if (user != null) {
                    CustomUserDetails details = new CustomUserDetails(user);

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(details, null,
                            details.getAuthorities());

                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }

            }

            chain.doFilter(request, response);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
            return; // Don't continue chain
        }
        // Note: TenantContext cleanup moved to TenantContextCleanupFilter
    }
}
