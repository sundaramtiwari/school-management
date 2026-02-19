package com.school.backend.fee;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.fee.dto.*;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.school.entity.School;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FeePaymentConcurrencyIntegrationTest extends BaseAuthenticatedIntegrationTest {

    private Long schoolId;
    private Long classId;
    private Long studentId;
    private Long feeTypeId;
    private Long feeStructureId;
    private Long sessionId;

    @Test
    void txA_and_txB_concurrent_payment_should_trigger_optimistic_lock() throws InterruptedException {
        setupData(BigDecimal.valueOf(10000));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        // Tx A pays 5000
        Future<ResponseEntity<FeePaymentDto>> futureA = executor.submit(() -> {
            try {
                latch.await();
                return pay(BigDecimal.valueOf(5000));
            } catch (Exception e) {
                return null;
            } finally {
                doneLatch.countDown();
            }
        });

        // Tx B pays 3000
        Future<ResponseEntity<FeePaymentDto>> futureB = executor.submit(() -> {
            try {
                latch.await();
                return pay(BigDecimal.valueOf(3000));
            } catch (Exception e) {
                return null;
            } finally {
                doneLatch.countDown();
            }
        });

        latch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);

        ResponseEntity<FeePaymentDto> resA = null;
        ResponseEntity<FeePaymentDto> resB = null;
        try {
            resA = futureA.get();
        } catch (Exception ignored) {
        }
        try {
            resB = futureB.get();
        } catch (Exception ignored) {
        }

        int successCount = 0;
        int failureCount = 0;

        if (resA != null && resA.getStatusCode() == HttpStatus.OK)
            successCount++;
        else
            failureCount++;

        if (resB != null && resB.getStatusCode() == HttpStatus.OK)
            successCount++;
        else
            failureCount++;

        // One must succeed, one must fail due to @Version on StudentFeeAssignment
        Assertions.assertThat(successCount).isEqualTo(1);
        Assertions.assertThat(failureCount).isEqualTo(1);

        // Check DB for lost updates
        BigDecimal totalPaidInDb = feePaymentRepository.findByStudentId(studentId).stream()
                .map(p -> p.getAmountPaid())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Should be either 5000 or 3000, not 8000
        BigDecimal expectedAmount = (resA != null && resA.getStatusCode() == HttpStatus.OK)
                ? BigDecimal.valueOf(5000)
                : BigDecimal.valueOf(3000);

        Assertions.assertThat(totalPaidInDb).isEqualByComparingTo(expectedAmount);

        BigDecimal principalPaidOnAssignment = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId)
                .stream()
                .map(a -> a.getPrincipalPaid())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Assertions.assertThat(principalPaidOnAssignment).isEqualByComparingTo(totalPaidInDb);

        executor.shutdown();
    }

    private ResponseEntity<FeePaymentDto> pay(BigDecimal amount) {
        FeePaymentRequest payReq = new FeePaymentRequest();
        payReq.setStudentId(studentId);
        payReq.setSessionId(sessionId);
        payReq.setAmountPaid(amount);
        payReq.setMode("CASH");

        HttpEntity<FeePaymentRequest> payEntity = new HttpEntity<>(payReq, headers);
        return restTemplate.exchange("/api/fees/payments", HttpMethod.POST, payEntity, FeePaymentDto.class);
    }

    private void setupData(BigDecimal feeAmount) {
        Map<String, Object> schoolReq = Map.of(
                "name", "Conc School " + System.currentTimeMillis(),
                "displayName", "CS",
                "board", "CBSE",
                "schoolCode", "CS-" + System.currentTimeMillis(),
                "city", "V", "state", "UP");

        HttpEntity<Map<String, Object>> schoolEntity = new HttpEntity<>(schoolReq, headers);
        ResponseEntity<School> schoolResp = restTemplate.exchange("/api/schools", HttpMethod.POST, schoolEntity,
                School.class);
        schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();

        loginAsSchoolAdmin(schoolId);
        sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
        setSessionHeader(sessionId);

        // Class
        Map<String, Object> classReq = Map.of("name", "10", "sessionId", sessionId, "schoolId", schoolId);
        ResponseEntity<Map> classResp = restTemplate.exchange("/api/classes", HttpMethod.POST,
                new HttpEntity<>(classReq, headers), Map.class);
        classId = Long.valueOf(classResp.getBody().get("id").toString());

        // Student
        StudentCreateRequest sreq = new StudentCreateRequest();
        sreq.setAdmissionNumber("C-" + System.currentTimeMillis());
        sreq.setFirstName("C");
        sreq.setGender(Gender.MALE);
        sreq.setGuardians(List.of(GuardianCreateRequest.builder().name("G").contactNumber("1234567890")
                .relation("FATHER").primaryGuardian(true).build()));
        studentId = restTemplate
                .exchange("/api/students", HttpMethod.POST, new HttpEntity<>(sreq, headers), StudentDto.class).getBody()
                .getId();

        // Fee Type
        FeeType typeReq = new FeeType();
        typeReq.setName("T");
        feeTypeId = restTemplate
                .exchange("/api/fees/types", HttpMethod.POST, new HttpEntity<>(typeReq, headers), FeeType.class)
                .getBody().getId();

        // Fee Structure
        FeeStructureCreateRequest fsReq = new FeeStructureCreateRequest();
        fsReq.setClassId(classId);
        fsReq.setSessionId(sessionId);
        fsReq.setFeeTypeId(feeTypeId);
        fsReq.setAmount(feeAmount);
        fsReq.setFrequency(com.school.backend.fee.enums.FeeFrequency.ONE_TIME);
        feeStructureId = restTemplate.exchange("/api/fees/structures", HttpMethod.POST,
                new HttpEntity<>(fsReq, headers), FeeStructureDto.class).getBody().getId();

        // Assign
        StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();
        assignReq.setStudentId(studentId);
        assignReq.setFeeStructureId(feeStructureId);
        assignReq.setSessionId(sessionId);
        restTemplate.exchange("/api/fees/assignments", HttpMethod.POST, new HttpEntity<>(assignReq, headers),
                StudentFeeAssignmentDto.class);
    }

    @AfterEach
    void cleanup() {
        fullCleanup();
    }
}
