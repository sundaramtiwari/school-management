package com.school.backend.core.teacher.repository;

import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.entity.Subject;
import com.school.backend.core.teacher.entity.TeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

        List<TeacherAssignment> findBySessionIdAndSchoolIdAndActiveTrue(Long sessionId, Long schoolId);

        @Query("SELECT DISTINCT ta.schoolClass FROM TeacherAssignment ta " +
                        "WHERE ta.teacher.id = :teacherId AND ta.session.id = :sessionId " +
                        "AND ta.schoolId = :schoolId AND ta.active = true")
        List<SchoolClass> findDistinctClassesByTeacherAndSession(
                        @Param("teacherId") Long teacherId, @Param("sessionId") Long sessionId,
                        @Param("schoolId") Long schoolId);

        @Query("SELECT DISTINCT ta.subject FROM TeacherAssignment ta " +
                        "WHERE ta.teacher.id = :teacherId AND ta.session.id = :sessionId " +
                        "AND ta.schoolClass.id = :classId AND ta.schoolId = :schoolId AND ta.active = true")
        List<Subject> findDistinctSubjectsByTeacherSessionAndClass(
                        @Param("teacherId") Long teacherId, @Param("sessionId") Long sessionId,
                        @Param("classId") Long classId, @Param("schoolId") Long schoolId);
}
