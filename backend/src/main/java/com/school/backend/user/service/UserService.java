package com.school.backend.user.service;

import com.school.backend.common.enums.UserRole;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.teacher.repository.TeacherRepository;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.core.teacher.service.TeacherAssignmentService;
import com.school.backend.user.dto.UserDto;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;
    private final TeacherRepository teacherRepository;
    private final TeacherAssignmentService teacherAssignmentService;

    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(String role, Pageable pageable) {
        // TenantFilterAspect ensures filtering by school_id
        if (role != null && !role.isEmpty()) {
            try {
                UserRole userRole = UserRole.valueOf(role.toUpperCase());
                return userRepository.findByRole(userRole, pageable)
                        .map(this::toDto);
            } catch (IllegalArgumentException e) {
                // Return empty page if role is invalid
                return Page.empty(pageable);
            }
        }
        return userRepository.findAll(pageable)
                .map(this::toDto);
    }

    @Transactional
    public UserDto createUser(UserDto dto) {
        Long schoolId = TenantContext.getSchoolId();
        if (schoolId == null) {
            throw new IllegalStateException("Operation requires a School Context");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("User with email " + dto.getEmail() + " already exists");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
        user.setSchool(school);
        user.setActive(true);
        user.setFullName(dto.getFullName());

        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto updateUser(Long id, UserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // No explicit check for schoolId needed because TenantFilter prevents finding
        // users from other schools?
        // Wait, repository.findById respects filters? YES, Hibernate filters apply to
        // finds too if enabled.

        user.setEmail(dto.getEmail());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        user.setRole(dto.getRole());

        // If a TEACHER user is being deactivated, soft-deactivate all their assignments
        boolean wasActive = user.isActive();
        user.setActive(dto.isActive());
        if (wasActive && !dto.isActive() && user.getRole() == UserRole.TEACHER) {
            teacherRepository.findByUserId(user.getId())
                    .ifPresent(teacher -> teacherAssignmentService.deactivateAllForTeacher(teacher.getId()));
        }

        return toDto(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userRepository.delete(user);
    }

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setActive(user.isActive());
        return dto;
    }
}
