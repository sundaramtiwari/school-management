package com.school.backend.fee;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.Gender;
import com.school.backend.common.enums.UserRole;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.student.dto.LedgerSummaryDto;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.fee.dto.FeeStatsDto;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.school.entity.School;
import com.school.backend.user.dto.AuthResponse;
import com.school.backend.user.dto.LoginRequest;
import com.school.backend.user.entity.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

public class FeeHardeningIntegrationTest extends BaseAuthenticatedIntegrationTest {

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Test
        void pending_should_match_dashboard_ledger_and_defaulter_amounts() throws Exception {
                Long schoolId = createSchoolAndLogin("FH-1");
                Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
                setSessionHeader(sessionId);
                Long studentId = seedStudentWithPending(schoolId, sessionId);

                ResponseEntity<FeeStatsDto> statsResponse = restTemplate.exchange(
                                "/api/fees/summary/stats",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                FeeStatsDto.class);
                Assertions.assertThat(statsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                Assertions.assertThat(Objects.requireNonNull(statsResponse.getBody()).getPendingDues())
                                .isEqualByComparingTo("620");

                ResponseEntity<LedgerSummaryDto[]> ledgerResponse = restTemplate.exchange(
                                "/api/students/" + studentId + "/ledger",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                LedgerSummaryDto[].class);
                Assertions.assertThat(ledgerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                LedgerSummaryDto[] ledgerRows = Objects.requireNonNull(ledgerResponse.getBody());
                Assertions.assertThat(ledgerRows).isNotEmpty();
                LedgerSummaryDto ledgerForSession = java.util.Arrays.stream(ledgerRows)
                                .filter(r -> sessionId.equals(r.getSessionId()))
                                .findFirst()
                                .orElseThrow();
                Assertions.assertThat(ledgerForSession.getTotalPending()).isEqualByComparingTo("620");

                ResponseEntity<String> defaulterResponse = restTemplate.exchange(
                                "/api/fees/defaulters?page=0&size=10",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                String.class);
                Assertions.assertThat(defaulterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                JsonNode root = objectMapper.readTree(defaulterResponse.getBody());
                JsonNode first = root.path("content").get(0);
                Assertions.assertThat(first).isNotNull();
                Assertions.assertThat(new BigDecimal(first.path("amountDue").asText())).isEqualByComparingTo("620");
        }

        @Test
        void fee_summary_stats_should_require_session_context_header() {
                Long schoolId = createSchoolAndLogin("FH-2");
                setupSession(schoolId, sessionRepository, schoolRepository);

                ResponseEntity<Map> response = restTemplate.exchange(
                                "/api/fees/summary/stats",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                Map.class);

                Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                Assertions.assertThat(Objects.requireNonNull(response.getBody()).get("message").toString())
                                .contains("Session context is missing");
        }

        @Test
        void teacher_should_not_be_allowed_to_create_fee_type() {
                Long schoolId = createSchoolAndLogin("FH-3");
                Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);

                School school = schoolRepository.findById(schoolId).orElseThrow();
                String email = "teacher-" + schoolId + "@test.com";
                String password = "teacher123";
                userRepository.save(User.builder()
                                .email(email)
                                .passwordHash(passwordEncoder.encode(password))
                                .role(UserRole.TEACHER)
                                .active(true)
                                .school(school)
                                .build());

                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setEmail(email);
                loginRequest.setPassword(password);
                ResponseEntity<AuthResponse> login = restTemplate.postForEntity("/api/auth/login", loginRequest,
                                AuthResponse.class);
                Assertions.assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);

                HttpHeaders teacherHeaders = new HttpHeaders();
                teacherHeaders.setBearerAuth(Objects.requireNonNull(login.getBody()).getToken());
                teacherHeaders.setContentType(MediaType.APPLICATION_JSON);
                teacherHeaders.set("X-Session-Id", String.valueOf(sessionId));

                FeeType request = new FeeType();
                request.setName("Unauthorized Type");

                ResponseEntity<Map> response = restTemplate.exchange(
                                "/api/fees/types",
                                HttpMethod.POST,
                                new HttpEntity<>(request, teacherHeaders),
                                Map.class);

                Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        private Long createSchoolAndLogin(String codeSuffix) {
                Map<String, Object> schoolReq = Map.of(
                                "name", "Fee Hardening School " + codeSuffix,
                                "displayName", "FHS-" + codeSuffix,
                                "board", "CBSE",
                                "schoolCode", "FHS-" + codeSuffix,
                                "city", "V",
                                "state", "UP");
                ResponseEntity<School> schoolResp = restTemplate.exchange(
                                "/api/schools", HttpMethod.POST, new HttpEntity<>(schoolReq, headers), School.class);
                Long schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
                loginAsSchoolAdmin(schoolId);
                return schoolId;
        }

        private Long seedStudentWithPending(Long schoolId, Long sessionId) {
                SchoolClass schoolClass = schoolClassRepository.save(SchoolClass.builder()
                                .schoolId(schoolId)
                                .sessionId(sessionId)
                                .name("10")
                                .section("A")
                                .active(true)
                                .build());

                Student student = studentRepository.save(Student.builder()
                                .schoolId(schoolId)
                                .admissionNumber("FH-" + schoolId)
                                .firstName("Pending")
                                .gender(Gender.MALE)
                                .active(true)
                                .currentClass(schoolClass)
                                .build());

                studentEnrollmentRepository.save(StudentEnrollment.builder()
                                .schoolId(schoolId)
                                .sessionId(sessionId)
                                .studentId(student.getId())
                                .classId(schoolClass.getId())
                                .section("A")
                                .enrollmentDate(LocalDate.of(2025, 4, 1))
                                .startDate(LocalDate.of(2025, 4, 1))
                                .active(true)
                                .build());

                FeeType feeType = feeTypeRepository.save(FeeType.builder()
                                .schoolId(schoolId)
                                .name("TUITION-" + schoolId)
                                .active(true)
                                .build());

                FeeStructure structure = feeStructureRepository.save(FeeStructure.builder()
                                .schoolId(schoolId)
                                .classId(schoolClass.getId())
                                .sessionId(sessionId)
                                .feeType(feeType)
                                .amount(BigDecimal.valueOf(1000))
                                .frequency(FeeFrequency.MONTHLY)
                                .active(true)
                                .build());

                assignmentRepository.save(StudentFeeAssignment.builder()
                                .schoolId(schoolId)
                                .studentId(student.getId())
                                .feeStructureId(structure.getId())
                                .sessionId(sessionId)
                                .amount(BigDecimal.valueOf(1000))
                                .lateFeeAccrued(BigDecimal.valueOf(100))
                                .totalDiscountAmount(BigDecimal.valueOf(50))
                                .lateFeeWaived(BigDecimal.valueOf(20))
                                .principalPaid(BigDecimal.valueOf(400))
                                .lateFeePaid(BigDecimal.valueOf(10))
                                .active(true)
                                .build());

                return student.getId();
        }

        @AfterEach
        void cleanup() {
                fullCleanup();
        }
}
