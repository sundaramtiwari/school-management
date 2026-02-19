package com.school.backend.fee.repository;

import com.school.backend.fee.entity.StudentFundingArrangement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentFundingArrangementRepository extends JpaRepository<StudentFundingArrangement, Long> {

    @Query("SELECT sfa FROM StudentFundingArrangement sfa WHERE sfa.studentId = :studentId AND sfa.sessionId = :sessionId AND sfa.active = true")
    Optional<StudentFundingArrangement> findActiveByStudentAndSession(Long studentId, Long sessionId);
}
