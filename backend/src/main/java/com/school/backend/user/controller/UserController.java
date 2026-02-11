package com.school.backend.user.controller;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.common.dto.PageResponseMapper;
import com.school.backend.user.dto.UserDto;
import com.school.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<PageResponse<UserDto>> list(Pageable pageable) {
        // Service layer enforces tenant isolation
        Page<UserDto> page = userService.listUsers(pageable);
        return ResponseEntity.ok(PageResponseMapper.fromPage(page));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<UserDto> create(@RequestBody UserDto dto) {
        // Service layer enforces: SCHOOL_ADMIN can only create users in their school
        return ResponseEntity.status(201).body(userService.createUser(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<UserDto> update(@PathVariable Long id, @RequestBody UserDto dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
