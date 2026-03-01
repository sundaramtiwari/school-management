package com.school.backend.core.teacher.repository;

import com.school.backend.core.teacher.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByUserId(Long userId);

    List<Teacher> findBySchoolId(Long schoolId);

    long countBySchoolId(Long schoolId);
}
