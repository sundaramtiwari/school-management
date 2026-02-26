package com.school.backend.testmanagement.repository;

import com.school.backend.testmanagement.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByClassIdAndSessionId(Long classId, Long sessionId);

    List<Exam> findBySchoolIdAndStartDateAfter(Long schoolId, java.time.LocalDate date);

    List<Exam> findBySchoolIdAndSessionIdAndStartDateAfter(Long schoolId, Long sessionId, java.time.LocalDate date);

    interface UpcomingExamView {
        String getName();

        LocalDate getStartDate();

        String getClassName();

        String getClassSection();
    }

    @Query("""
            SELECT e.name as name,
                   e.startDate as startDate,
                   c.name as className,
                   c.section as classSection
            FROM Exam e
            JOIN SchoolClass c ON c.id = e.classId
            WHERE e.schoolId = :schoolId
              AND e.sessionId = :sessionId
              AND e.startDate > :date
            ORDER BY e.startDate ASC
            """)
    List<UpcomingExamView> findUpcomingExamViews(
            @Param("schoolId") Long schoolId,
            @Param("sessionId") Long sessionId,
            @Param("date") LocalDate date,
            Pageable pageable);

    @Query("""
            select count(e) from Exam e
            where e.sessionId = :sessionId
              and e.classId = :classId
              and e.status <> com.school.backend.common.enums.ExamStatus.LOCKED
            """)
    long countNonLockedBySessionIdAndClassId(@Param("sessionId") Long sessionId, @Param("classId") Long classId);
}
