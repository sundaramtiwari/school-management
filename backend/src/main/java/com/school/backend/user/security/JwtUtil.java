package com.school.backend.user.security;

import com.school.backend.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private static final long EXPIRY =
            24 * 60 * 60 * 1000;

    private SecretKey getKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        return Keys.hmacShaKeyFor(
                keyBytes
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

                .signWith(getKey(), SignatureAlgorithm.HS256)

                .compact();
    }

    public Claims parse(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
