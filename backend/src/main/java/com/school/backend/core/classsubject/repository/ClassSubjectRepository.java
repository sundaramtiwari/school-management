package com.school.backend.core.classsubject.repository;

import com.school.backend.core.classsubject.entity.ClassSubject;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Long> {

        Page<ClassSubject> findBySchoolClassId(Long classId, Pageable pageable);

        Page<ClassSubject> findBySchoolId(Long schoolId, Pageable pageable);

        Optional<ClassSubject> findBySchoolClassIdAndSubjectId(Long classId, Long subjectId);

        boolean existsBySchoolClassIdAndSubjectId(Long classId, Long subjectId);

        @Query("""
                        select cs from ClassSubject cs
                        where cs.schoolClass.sessionId = :sessionId
                          and cs.schoolId = :schoolId
                          and cs.teacher is not null
                        """)
        List<ClassSubject> findAssignmentsBySession(@Param("sessionId") Long sessionId,
                        @Param("schoolId") Long schoolId);

        @Query("""
                        select cs from ClassSubject cs
                        where cs.teacher.id = :teacherId
                          and cs.schoolClass.sessionId = :sessionId
                          and cs.schoolId = :schoolId
                        """)
        List<ClassSubject> findByTeacherIdAndSessionId(@Param("teacherId") Long teacherId,
                        @Param("sessionId") Long sessionId,
                        @Param("schoolId") Long schoolId);

        @Query("""
                        select distinct cs.schoolClass from ClassSubject cs
                        where cs.teacher.id = :teacherId
                          and cs.schoolClass.sessionId = :sessionId
                          and cs.schoolId = :schoolId
                        """)
        List<SchoolClass> findDistinctClassesByTeacherAndSession(@Param("teacherId") Long teacherId,
                        @Param("sessionId") Long sessionId, @Param("schoolId") Long schoolId);

        @Query("""
                        select distinct cs.subject from ClassSubject cs
                        where cs.teacher.id = :teacherId
                          and cs.schoolClass.sessionId = :sessionId
                          and cs.schoolClass.id = :classId
                          and cs.schoolId = :schoolId
                        """)
        List<Subject> findDistinctSubjectsByTeacherSessionAndClass(@Param("teacherId") Long teacherId,
                        @Param("sessionId") Long sessionId, @Param("classId") Long classId,
                        @Param("schoolId") Long schoolId);

        boolean existsBySchoolClassSessionIdAndTeacherIdAndSchoolClassIdAndSubjectIdAndSchoolId(
                        Long sessionId, Long teacherId, Long classId, Long subjectId, Long schoolId);

        List<ClassSubject> findByTeacherIdAndSchoolClassSessionIdAndSchoolId(Long teacherId, Long sessionId,
                        Long schoolId);
}
