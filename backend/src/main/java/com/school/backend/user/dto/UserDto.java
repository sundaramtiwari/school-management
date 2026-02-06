package com.school.backend.user.dto;

import com.school.backend.common.enums.UserRole;
import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String fullName;
    private String password; // Only for creation/update
    private UserRole role;
    private boolean active;
}
