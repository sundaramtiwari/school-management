package com.school.backend.fee;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.FeeFrequency;
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

public class FeeConcurrencyIntegrationTest extends BaseAuthenticatedIntegrationTest {

    private Long schoolId;
    private Long classId;
    private Long studentId;
    private Long feeTypeId;
    private Long feeStructureId;
    private Long sessionId;

    @Test
    void concurrent_payments_should_be_consistent() throws InterruptedException {
        setupData(BigDecimal.valueOf(10000));

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<ResponseEntity<FeePaymentDto>>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    Long assignmentId = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId)
                            .get(0).getId();

                    FeePaymentRequest payReq = new FeePaymentRequest();
                    payReq.setStudentId(studentId);
                    payReq.setSessionId(sessionId);
                    payReq.setAllocations(List.of(
                            FeePaymentAllocationRequest.builder()
                                    .assignmentId(assignmentId)
                                    .principalAmount(BigDecimal.valueOf(100))
                                    .build()));
                    payReq.setMode("CASH");

                    HttpEntity<FeePaymentRequest> payEntity = new HttpEntity<>(payReq, headers);
                    return restTemplate.exchange("/api/fees/payments", HttpMethod.POST, payEntity, FeePaymentDto.class);
                } catch (Exception e) {
                    return null;
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        latch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);

        for (Future<ResponseEntity<FeePaymentDto>> future : futures) {
            try {
                ResponseEntity<FeePaymentDto> res = future.get();
                if (res != null && res.getStatusCode() == HttpStatus.OK) {
                    successCount.incrementAndGet();
                }
            } catch (Exception e) {
                // Ignore failure
            }
        }

        executor.shutdown();

        BigDecimal totalPaidInDb = feePaymentRepository.findByStudentId(studentId).stream()
                .map(p -> p.getAmountPaid())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Assertions.assertThat(totalPaidInDb).isEqualByComparingTo(BigDecimal.valueOf(successCount.get() * 100L));

        BigDecimal principalPaidOnAssignment = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId)
                .stream()
                .map(a -> a.getPrincipalPaid())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Assertions.assertThat(principalPaidOnAssignment).isEqualByComparingTo(totalPaidInDb);
    }

    @Test
    void overpayment_protection_should_be_enforced_concurrently() throws InterruptedException {
        // Setup with small fee
        setupData(BigDecimal.valueOf(500));

        int threadCount = 5; // Each tries to pay 500
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<ResponseEntity<FeePaymentDto>>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    Long assignmentId = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId)
                            .get(0).getId();

                    FeePaymentRequest payReq = new FeePaymentRequest();
                    payReq.setStudentId(studentId);
                    payReq.setSessionId(sessionId);
                    payReq.setAllocations(List.of(
                            FeePaymentAllocationRequest.builder()
                                    .assignmentId(assignmentId)
                                    .principalAmount(BigDecimal.valueOf(500))
                                    .build()));
                    payReq.setMode("CASH");

                    HttpEntity<FeePaymentRequest> payEntity = new HttpEntity<>(payReq, headers);
                    return restTemplate.exchange("/api/fees/payments", HttpMethod.POST, payEntity, FeePaymentDto.class);
                } catch (Exception e) {
                    return null;
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        latch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);

        for (Future<ResponseEntity<FeePaymentDto>> future : futures) {
            try {
                ResponseEntity<FeePaymentDto> res = future.get();
                if (res != null) {
                    if (res.getStatusCode() == HttpStatus.OK) {
                        successCount.incrementAndGet();
                    } else if (res.getStatusCode() == HttpStatus.BAD_REQUEST
                            || res.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                        // Optimistic locking failure might return 500 in some setups, or 400 if
                        // translated.
                        failureCount.incrementAndGet();
                    }
                }
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        }

        executor.shutdown();

        // Exactly one should succeed since they all try to pay the full amount
        Assertions.assertThat(successCount.get()).isEqualTo(1);

        BigDecimal totalPaidInDb = feePaymentRepository.findByStudentId(studentId).stream()
                .map(p -> p.getAmountPaid())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Assertions.assertThat(totalPaidInDb).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    private void setupData(BigDecimal feeAmount) {
        Map<String, Object> schoolReq = Map.of(
                "name", "Concurrency School " + System.currentTimeMillis(),
                "displayName", "CS",
                "board", "CBSE",
                "schoolCode", "CS-" + System.currentTimeMillis(),
                "city", "Varanasi",
                "state", "UP");

        HttpEntity<Map<String, Object>> schoolEntity = new HttpEntity<>(schoolReq, headers);
        ResponseEntity<School> schoolResp = restTemplate.exchange("/api/schools", HttpMethod.POST, schoolEntity,
                School.class);
        Assertions.assertThat(schoolResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();

        loginAsSchoolAdmin(schoolId);
        sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
        setSessionHeader(sessionId);

        // Class
        Map<String, Object> classReq = Map.of("name", "10", "sessionId", sessionId, "schoolId", schoolId);
        HttpEntity<Map<String, Object>> classEntity = new HttpEntity<>(classReq, headers);
        ResponseEntity<Map<String, Object>> classResp = restTemplate.exchange("/api/classes", HttpMethod.POST,
                classEntity, getMapType());
        Assertions.assertThat(classResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        classId = Long.valueOf(classResp.getBody().get("id").toString());

        // Student
        StudentCreateRequest sreq = new StudentCreateRequest();
        sreq.setAdmissionNumber("CONC-" + System.currentTimeMillis());
        sreq.setFirstName("Conc");
        sreq.setGender(Gender.MALE);
        sreq.setGuardians(List.of(GuardianCreateRequest.builder().name("G").contactNumber("1234567890")
                .relation("FATHER").primaryGuardian(true).build()));
        HttpEntity<StudentCreateRequest> studentEntity = new HttpEntity<>(sreq, headers);
        ResponseEntity<StudentDto> studentResp = restTemplate.exchange("/api/students", HttpMethod.POST, studentEntity,
                StudentDto.class);
        Assertions.assertThat(studentResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        studentId = Objects.requireNonNull(studentResp.getBody()).getId();

        // Fee Type
        FeeType typeReq = new FeeType();
        typeReq.setName("TUITION");
        HttpEntity<FeeType> typeEntity = new HttpEntity<>(typeReq, headers);
        ResponseEntity<FeeType> typeResp = restTemplate.exchange("/api/fees/types", HttpMethod.POST, typeEntity,
                FeeType.class);
        Assertions.assertThat(typeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        feeTypeId = Objects.requireNonNull(typeResp.getBody()).getId();

        // Fee Structure
        FeeStructureCreateRequest fsReq = new FeeStructureCreateRequest();
        fsReq.setClassId(classId);
        fsReq.setSessionId(sessionId);
        fsReq.setFeeTypeId(feeTypeId);
        fsReq.setAmount(feeAmount);
        fsReq.setFrequency(FeeFrequency.ONE_TIME);
        HttpEntity<FeeStructureCreateRequest> fsEntity = new HttpEntity<>(fsReq, headers);
        ResponseEntity<FeeStructureDto> fsResp = restTemplate.exchange("/api/fees/structures", HttpMethod.POST,
                fsEntity, FeeStructureDto.class);
        Assertions.assertThat(fsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        feeStructureId = Objects.requireNonNull(fsResp.getBody()).getId();

        // Assign
        StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();
        assignReq.setStudentId(studentId);
        assignReq.setFeeStructureId(feeStructureId);
        assignReq.setSessionId(sessionId);
        HttpEntity<StudentFeeAssignRequest> assignEntity = new HttpEntity<>(assignReq, headers);
        ResponseEntity<StudentFeeAssignmentDto> assignResp = restTemplate.exchange("/api/fees/assignments",
                HttpMethod.POST, assignEntity, StudentFeeAssignmentDto.class);
        Assertions.assertThat(assignResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @SuppressWarnings("unchecked")
    private Class<Map<String, Object>> getMapType() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }

    @AfterEach
    void cleanup() {
        fullCleanup();
    }
}
