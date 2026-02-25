package com.school.backend.user.controller;

import com.school.backend.user.dto.AuthResponse;
import com.school.backend.user.dto.LoginRequest;
import com.school.backend.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
