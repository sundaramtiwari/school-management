package com.school.backend.user.security;

import com.school.backend.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET =
            "school-management-jwt-secret-key-2026-very-secure-123456";

    private static final long EXPIRY =
            24 * 60 * 60 * 1000;

    private final SecretKey key;

    public JwtUtil() {
        this.key = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generate(User user) {

        return Jwts.builder()

                .setSubject(user.getEmail())

                .claim("userId", user.getId())

                .claim("schoolId",
                        user.getSchool() != null
                                ? user.getSchool().getId()
                                : null)

                .claim("role", user.getRole().name())

                .setIssuedAt(new Date())

                .setExpiration(
                        new Date(System.currentTimeMillis() + EXPIRY)
                )

                .signWith(key, SignatureAlgorithm.HS256)

                .compact();
    }

    public Claims parse(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
