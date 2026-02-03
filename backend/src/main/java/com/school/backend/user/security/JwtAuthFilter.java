package com.school.backend.user.security;

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

    public JwtAuthFilter(JwtUtil jwtUtil,
                         UserRepository userRepository) {

        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            try {
                Claims claims = jwtUtil.parse(token);

                String email = claims.getSubject();

                User user = userRepository
                        .findByEmailAndActiveTrue(email)
                        .orElse(null);

                if (user != null) {

                    CustomUserDetails details =
                            new CustomUserDetails(user);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    details,
                                    null,
                                    details.getAuthorities()
                            );

                    auth.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(auth);
                }

            } catch (Exception e) {
                // Invalid token → ignore, user unauthenticated
            }
        }

        chain.doFilter(request, response);
    }
}
