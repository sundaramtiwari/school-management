package com.school.backend.user.dto;

import com.school.backend.common.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private UserRole role;
    private Long schoolId;
    private Long userId;
}
