package com.school.backend.core.teacher.service;

import com.school.backend.core.teacher.dto.TeacherListItemDto;
import com.school.backend.core.teacher.entity.Teacher;
import com.school.backend.core.teacher.repository.TeacherRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public List<TeacherListItemDto> listTeachersBySchoolId() {
        Long schoolId = SecurityUtil.schoolId();
        List<Teacher> teachers = teacherRepository.findBySchoolId(schoolId);
        return teachers.stream()
                .map(this::toListItemDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeacherListItemDto getByUserId(Long userId) {
        Long schoolId = SecurityUtil.schoolId();
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new com.school.backend.common.exception.ResourceNotFoundException(
                        "Teacher not found for user: " + userId));

        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Teacher does not belong to current school");
        }

        return toListItemDto(teacher);
    }

    private TeacherListItemDto toListItemDto(Teacher t) {
        TeacherListItemDto dto = new TeacherListItemDto();
        dto.setId(t.getId());
        dto.setUserId(t.getUser() != null ? t.getUser().getId() : null);
        dto.setFullName(t.getUser() != null && t.getUser().getFullName() != null ? t.getUser().getFullName() : "");
        return dto;
    }
}
