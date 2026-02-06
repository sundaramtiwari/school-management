package com.school.backend.fee.repository;

import com.school.backend.fee.entity.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {
    List<FeePayment> findByStudentId(Long studentId);

    @Query("SELECT SUM(f.amountPaid) FROM FeePayment f WHERE f.schoolId = :schoolId AND f.paymentDate = :date")
    Long sumAmountPaidBySchoolIdAndPaymentDate(@Param("schoolId") Long schoolId, @Param("date") LocalDate date);

    @Query("SELECT SUM(f.amountPaid) FROM FeePayment f WHERE f.schoolId = :schoolId")
    Long sumAmountPaidBySchoolId(@Param("schoolId") Long schoolId);

    List<FeePayment> findTop10BySchoolIdOrderByPaymentDateDesc(Long schoolId);
}
