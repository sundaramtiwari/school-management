package com.school.backend.user.controller;

import com.school.backend.user.dto.AuthResponse;
import com.school.backend.user.dto.LoginRequest;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import com.school.backend.user.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {

        User user = userRepository
                .findByEmailAndActiveTrue(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid credentials")
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())
        ) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generate(user);

        return new AuthResponse(
                token,
                user.getRole(),
                user.getSchool() != null
                        ? user.getSchool().getId()
                        : null,
                user.getId()
        );
    }
}
