package com.school.backend.user.security;

import com.school.backend.user.entity.User;
import com.school.backend.common.enums.UserRole;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public Long getUserId() {
        return user.getId();
    }

    public Long getSchoolId() {
        return user.getSchool() != null
            ? user.getSchool().getId()
            : null;
    }

    public UserRole getRole() {
        return user.getRole();
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {

        return List.of(
            new SimpleGrantedAuthority(
                "ROLE_" + user.getRole().name()
            )
        );
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
