package com.school.backend.core.teacher.repository;

import com.school.backend.core.teacher.entity.TeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {

    List<TeacherAssignment> findByTeacherIdAndSessionIdAndActiveTrue(Long teacherId, Long sessionId);

    boolean existsByTeacherIdAndSessionIdAndSchoolClassIdAndSubjectIdAndActiveTrue(
            Long teacherId, Long sessionId, Long schoolClassId, Long subjectId);

    boolean existsByTeacherIdAndSessionIdAndSchoolClassIdAndActiveTrue(
            Long teacherId, Long sessionId, Long schoolClassId);

    List<TeacherAssignment> findBySessionIdAndSchoolClassId(Long sessionId, Long schoolClassId);

    List<TeacherAssignment> findByTeacherIdAndActiveTrue(Long teacherId);

    List<TeacherAssignment> findBySessionIdAndActiveTrue(Long sessionId);

}
