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
    public List<TeacherListItemDto> listTeachers() {
        Long schoolId = SecurityUtil.schoolId();
        List<Teacher> teachers = teacherRepository.findBySchoolId(schoolId);
        return teachers.stream()
                .map(this::toListItemDto)
                .collect(Collectors.toList());
    }

    private TeacherListItemDto toListItemDto(Teacher t) {
        TeacherListItemDto dto = new TeacherListItemDto();
        dto.setId(t.getId());
        dto.setFullName(t.getUser() != null && t.getUser().getFullName() != null ? t.getUser().getFullName() : "");
        return dto;
    }
}
