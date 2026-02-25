package com.school.backend.core.dashboard;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.ExpensePaymentMode;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.dashboard.dto.DailyCashDashboardDto;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.expense.dto.ExpenseHeadCreateRequest;
import com.school.backend.expense.dto.ExpenseHeadTotalDto;
import com.school.backend.expense.dto.ExpenseVoucherCreateRequest;
import com.school.backend.fee.dto.FeePaymentAllocationRequest;
import com.school.backend.fee.dto.FeePaymentRequest;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.School;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DailyCashDashboardIntegrationTest extends BaseAuthenticatedIntegrationTest {

        @org.springframework.beans.factory.annotation.Autowired
        private StudentFeeAssignmentRepository assignmentRepository;

        @Test
        void daily_cash_dashboard_should_include_only_cash_mode_data() {
                ResponseEntity<School> schoolResp = restTemplate.exchange(
                                "/api/schools",
                                HttpMethod.POST,
                                new HttpEntity<>(Map.of(
                                                "name", "Cash Dash School",
                                                "displayName", "CDS",
                                                "board", "CBSE",
                                                "schoolCode", "CDS-001"), headers),
                                School.class);
                Long schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
                loginAsSchoolAdmin(schoolId);

                Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
                setSessionHeader(sessionId);

                // class
                ResponseEntity<Map> classResp = restTemplate.exchange(
                                "/api/classes",
                                HttpMethod.POST,
                                new HttpEntity<>(Map.of("name", "8", "sessionId", sessionId, "schoolId", schoolId),
                                                headers),
                                Map.class);
                Long classId = Long.valueOf(Objects.requireNonNull(classResp.getBody()).get("id").toString());

                // student
                StudentCreateRequest sreq = new StudentCreateRequest();
                sreq.setAdmissionNumber("ADM-CASH-1");
                sreq.setFirstName("Cash");
                sreq.setGender(Gender.MALE);
                sreq.setGuardians(List.of(GuardianCreateRequest.builder()
                                .name("G")
                                .contactNumber("9999999999")
                                .relation("FATHER")
                                .primaryGuardian(true)
                                .build()));
                Long studentId = Objects.requireNonNull(restTemplate
                                .exchange("/api/students", HttpMethod.POST, new HttpEntity<>(sreq, headers),
                                                StudentDto.class)
                                .getBody()).getId();

                // fee type
                FeeType feeType = new FeeType();
                feeType.setName("Tuition");
                Long feeTypeId = Objects.requireNonNull(restTemplate
                                .exchange("/api/fees/types", HttpMethod.POST, new HttpEntity<>(feeType, headers),
                                                FeeType.class)
                                .getBody()).getId();

                // fee structure
                FeeStructureCreateRequest fsReq = new FeeStructureCreateRequest();
                fsReq.setClassId(classId);
                fsReq.setSessionId(sessionId);
                fsReq.setFeeTypeId(feeTypeId);
                fsReq.setAmount(new BigDecimal("1000.00"));
                fsReq.setFrequency(FeeFrequency.ONE_TIME);
                Long feeStructureId = Objects.requireNonNull(restTemplate.exchange(
                                "/api/fees/structures",
                                HttpMethod.POST,
                                new HttpEntity<>(fsReq, headers),
                                FeeStructureDto.class).getBody()).getId();

                // assign
                StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();
                assignReq.setStudentId(studentId);
                assignReq.setFeeStructureId(feeStructureId);
                assignReq.setSessionId(sessionId);
                assignReq.setDueDate(LocalDate.of(2026, 2, 15));
                restTemplate.exchange("/api/fees/assignments", HttpMethod.POST, new HttpEntity<>(assignReq, headers),
                                Object.class);

                LocalDate date = LocalDate.of(2026, 2, 20);

                // CASH payment = 500
                Long assignmentId = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId)
                                .get(0).getId();

                FeePaymentRequest cashPayment = new FeePaymentRequest();
                cashPayment.setStudentId(studentId);
                cashPayment.setSessionId(sessionId);
                cashPayment.setAllocations(List.of(
                                FeePaymentAllocationRequest.builder()
                                                .assignmentId(assignmentId)
                                                .principalAmount(new BigDecimal("500.00"))
                                                .build()));
                cashPayment.setMode("CASH");
                cashPayment.setPaymentDate(date);
                restTemplate.exchange("/api/fees/payments", HttpMethod.POST, new HttpEntity<>(cashPayment, headers),
                                Object.class);

                // UPI payment = 100 (must be excluded)
                FeePaymentRequest upiPayment = new FeePaymentRequest();
                upiPayment.setStudentId(studentId);
                upiPayment.setSessionId(sessionId);
                upiPayment.setAllocations(List.of(
                                FeePaymentAllocationRequest.builder()
                                                .assignmentId(assignmentId)
                                                .principalAmount(new BigDecimal("100.00"))
                                                .build()));
                upiPayment.setMode("UPI");
                upiPayment.setPaymentDate(date);
                restTemplate.exchange("/api/fees/payments", HttpMethod.POST, new HttpEntity<>(upiPayment, headers),
                                Object.class);

                // expense head
                ExpenseHeadCreateRequest headReq = new ExpenseHeadCreateRequest();
                headReq.setName("Stationery");
                ResponseEntity<Map> headResp = restTemplate.exchange(
                                "/api/expenses/heads",
                                HttpMethod.POST,
                                new HttpEntity<>(headReq, headers),
                                Map.class);
                Long headId = Long.valueOf(Objects.requireNonNull(headResp.getBody()).get("id").toString());

                // CASH expense = 200
                ExpenseVoucherCreateRequest cashExpense = new ExpenseVoucherCreateRequest();
                cashExpense.setExpenseDate(date);
                cashExpense.setExpenseHeadId(headId);
                cashExpense.setAmount(new BigDecimal("200.00"));
                cashExpense.setPaymentMode(ExpensePaymentMode.CASH);
                restTemplate.exchange("/api/expenses", HttpMethod.POST, new HttpEntity<>(cashExpense, headers),
                                Object.class);

                // UPI expense = 50 (must be excluded)
                ExpenseVoucherCreateRequest upiExpense = new ExpenseVoucherCreateRequest();
                upiExpense.setExpenseDate(date);
                upiExpense.setExpenseHeadId(headId);
                upiExpense.setAmount(new BigDecimal("50.00"));
                upiExpense.setPaymentMode(ExpensePaymentMode.UPI);
                restTemplate.exchange("/api/expenses", HttpMethod.POST, new HttpEntity<>(upiExpense, headers),
                                Object.class);

                ResponseEntity<DailyCashDashboardDto> response = restTemplate.exchange(
                                "/api/dashboard/daily-cash?date=2026-02-20",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                DailyCashDashboardDto.class);

                Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DailyCashDashboardDto body = Objects.requireNonNull(response.getBody());
        Assertions.assertThat(body.getTotalFeeCollected()).isEqualByComparingTo(new BigDecimal("500.00"));
        Assertions.assertThat(body.getTotalExpense()).isEqualByComparingTo(new BigDecimal("200.00"));
        Assertions.assertThat(body.getCashRevenue()).isEqualByComparingTo(new BigDecimal("500.00"));
        Assertions.assertThat(body.getBankRevenue()).isEqualByComparingTo(new BigDecimal("100.00"));
        Assertions.assertThat(body.getCashExpense()).isEqualByComparingTo(new BigDecimal("200.00"));
        Assertions.assertThat(body.getBankExpense()).isEqualByComparingTo(new BigDecimal("50.00"));
        Assertions.assertThat(body.getNetCash()).isEqualByComparingTo(new BigDecimal("300.00"));
        Assertions.assertThat(body.getNetBank()).isEqualByComparingTo(new BigDecimal("50.00"));
        Assertions.assertThat(body.getNetAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        Assertions.assertThat(body.getHeadWiseCollection()).hasSize(1);
        Assertions.assertThat(body.getHeadWiseCollection().get(0).getTotalCollected())
                .isEqualByComparingTo(new BigDecimal("500.00"));
                Assertions.assertThat(body.getExpenseBreakdown()).hasSize(1);
                ExpenseHeadTotalDto expenseHeadTotal = body.getExpenseBreakdown().get(0);
                Assertions.assertThat(expenseHeadTotal.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        }

        @AfterEach
        void cleanup() {
                fullCleanup();
        }
}
