package com.school.backend.core.student;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.PromotionType;
import com.school.backend.core.classsubject.dto.SchoolClassCreateRequest;
import com.school.backend.core.classsubject.dto.SchoolClassDto;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.*;
import com.school.backend.core.student.repository.PromotionRecordRepository;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.FeePaymentAllocationRequest;
import com.school.backend.fee.dto.FeePaymentRequest;
import com.school.backend.fee.dto.FeePaymentDto;
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.dto.StudentFeeAssignmentDto;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.dto.SchoolDto;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.school.backend.common.enums.Gender.MALE;
import static org.assertj.core.api.Assertions.assertThat;

public class PromotionFlowIntegrationTest extends BaseAuthenticatedIntegrationTest {

        private static final String FEE_TYPE_TUITION = "TUITION";
        private static final String PAYMENT_MODE_CASH = "CASH";
        private static final String BOARD_CBSE = "CBSE";

        @Autowired
        private FeePaymentRepository feePaymentRepository;
        @Autowired
        private FeeStructureRepository feeStructureRepository;
        @Autowired
        private FeeTypeRepository feeTypeRepository;
        @Autowired
        private StudentFeeAssignmentRepository assignmentRepository;
        @Autowired
        private PromotionRecordRepository promotionRecordRepository;
        @Autowired
        private StudentEnrollmentRepository studentEnrollmentRepository;
        @Autowired
        private StudentRepository studentRepository;
        @Autowired
        private SchoolClassRepository schoolClassRepository;
        @Autowired
        private SchoolRepository schoolRepository;
        @Autowired
        private AcademicSessionRepository sessionRepository;
        @Autowired
        private UserRepository userRepository;
        private Long schoolId;
        private Long feeTypeId;
        private Long feeStructureId;
        private Long fromClassId;
        private Long toClassId;
        private Long studentId;
        private Long session2024Id;
        private Long session2025Id;

        @BeforeEach
        void setup() {
                this.schoolId = createSchool("Test School");
                // LOGIN AS SCHOOL ADMIN NOW
                loginAsSchoolAdmin(schoolId);

                // Create sessions
                AcademicSession session2024 = sessionRepository.save(AcademicSession.builder()
                                .name("2024-25")
                                .startDate(LocalDate.of(2024, 4, 1))
                                .endDate(LocalDate.of(2025, 3, 31))
                                .schoolId(schoolId)
                                .active(true)
                                .build());
                session2024Id = session2024.getId();

                AcademicSession session2025 = sessionRepository.save(AcademicSession.builder()
                                .name("2025-26")
                                .startDate(LocalDate.of(2025, 4, 1))
                                .endDate(LocalDate.of(2026, 3, 31))
                                .schoolId(schoolId)
                                .active(true)
                                .build());
                session2025Id = session2025.getId();

                setSessionHeader(session2024Id); // Set default session for student registration

                this.fromClassId = createClass(schoolId, "Class 1", "A", session2024Id);
                this.toClassId = createClass(schoolId, "Class 2", "B", session2025Id);
                this.studentId = createStudent(schoolId, "Amit Kumar");

                enrollStudent(studentId, fromClassId, "A", session2024Id);
        }

        // ------------------------------ TEST ----------------------------------

        @Test
        void testPromotionFlow() {

                // --------- Setup Fee ---------

                // Create FeeType
                FeeType type = new FeeType();
                type.setName(FEE_TYPE_TUITION);

                HttpEntity<FeeType> typeEntity = new HttpEntity<>(type, headers);

                ResponseEntity<FeeType> typeResp = restTemplate.exchange(
                                "/api/fees/types",
                                HttpMethod.POST,
                                typeEntity,
                                FeeType.class);

                feeTypeId = Objects.requireNonNull(typeResp.getBody()).getId();

                // Create FeeStructure
                FeeStructureCreateRequest fsReq = new FeeStructureCreateRequest();

                fsReq.setClassId(toClassId);
                fsReq.setSessionId(session2025Id);
                fsReq.setFeeTypeId(feeTypeId);
                fsReq.setAmount(java.math.BigDecimal.valueOf(10000));
                fsReq.setFrequency(FeeFrequency.ONE_TIME);

                HttpEntity<FeeStructureCreateRequest> fsEntity = new HttpEntity<>(fsReq, headers);

                ResponseEntity<FeeStructureDto> fsResp = restTemplate.exchange(
                                "/api/fees/structures",
                                HttpMethod.POST,
                                fsEntity,
                                FeeStructureDto.class);

                feeStructureId = Objects.requireNonNull(fsResp.getBody()).getId();

                // Assign Fee
                StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();

                assignReq.setStudentId(studentId);
                assignReq.setFeeStructureId(feeStructureId);
                assignReq.setSessionId(session2025Id);

                HttpEntity<StudentFeeAssignRequest> assignEntity = new HttpEntity<>(assignReq, headers);

                restTemplate.exchange(
                                "/api/fees/assignments",
                                HttpMethod.POST,
                                assignEntity,
                                StudentFeeAssignmentDto.class);

                // Pay Full Fee
                Long assignmentId = assignmentRepository.findByStudentIdAndSessionId(studentId, session2025Id)
                                .get(0).getId();

                FeePaymentRequest payReq = new FeePaymentRequest();

                payReq.setStudentId(studentId);
                payReq.setSessionId(session2025Id);
                payReq.setAllocations(List.of(
                                FeePaymentAllocationRequest.builder()
                                                .assignmentId(assignmentId)
                                                .principalAmount(java.math.BigDecimal.valueOf(10000))
                                                .build()));
                payReq.setMode(PAYMENT_MODE_CASH);

                HttpEntity<FeePaymentRequest> payEntity = new HttpEntity<>(payReq, headers);

                restTemplate.exchange(
                                "/api/fees/payments",
                                HttpMethod.POST,
                                payEntity,
                                FeePaymentDto.class);

                // Promote
                PromotionRequest promoteReq = new PromotionRequest();

                promoteReq.setStudentIds(List.of(studentId));
                promoteReq.setTargetClassId(toClassId);
                promoteReq.setTargetSessionId(session2025Id);
                promoteReq.setPromotionType(PromotionType.PROMOTE);
                promoteReq.setRemarks("Promoted successfully");

                HttpEntity<PromotionRequest> promoteEntity = new HttpEntity<>(promoteReq, headers);

                ResponseEntity<Void> promoteResponse = restTemplate.exchange(
                                "/api/promotions",
                                HttpMethod.POST,
                                promoteEntity,
                                Void.class);

                assertThat(promoteResponse.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                assertThat(promotionRecordRepository.findByStudentIdOrderByPromotedAtAsc(studentId))
                                .hasSize(1);

                // Enrollment history
                HttpEntity<Void> historyEntity = new HttpEntity<>(headers);
                ResponseEntity<java.util.List<StudentEnrollmentDto>> enrollmentListResponse = restTemplate.exchange(
                                "/api/students/" + studentId + "/history/enrollments",
                                HttpMethod.GET,
                                historyEntity,
                                new ParameterizedTypeReference<>() {
                                });

                assertThat(enrollmentListResponse.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                assertThat(Objects.requireNonNull(enrollmentListResponse.getBody()))
                                .hasSize(2);

                StudentEnrollmentDto latest = enrollmentListResponse.getBody()
                                .stream()
                                .max(Comparator.comparing(StudentEnrollmentDto::getId))
                                .orElseThrow();

                assertThat(latest.getClassId()).isEqualTo(toClassId);
                assertThat(latest.getSection()).isEqualTo("B");
                assertThat(latest.getSessionId()).isEqualTo(session2025Id);
        }

        // ------------------------------------------------------------------------

        private Long createSchool(String name) {

                SchoolCreateRequest req = new SchoolCreateRequest();

                req.setName(name);
                req.setDisplayName(name);
                req.setBoard(BOARD_CBSE);
                req.setAddress("Varanasi");

                HttpEntity<SchoolCreateRequest> entity = new HttpEntity<>(req, headers);

                ResponseEntity<SchoolDto> res = restTemplate.exchange(
                                "/api/schools",
                                HttpMethod.POST,
                                entity,
                                SchoolDto.class);

                assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);

                return Objects.requireNonNull(res.getBody()).getId();
        }

        private Long createClass(Long schoolId,
                        String className,
                        String section,
                        Long sessionId) {

                SchoolClassCreateRequest req = new SchoolClassCreateRequest();

                req.setName(className);
                req.setSection(section);
                req.setSessionId(sessionId);
                req.setSchoolId(schoolId);

                HttpEntity<SchoolClassCreateRequest> entity = new HttpEntity<>(req, headers);

                ResponseEntity<SchoolClassDto> res = restTemplate.exchange(
                                "/api/classes",
                                HttpMethod.POST,
                                entity,
                                SchoolClassDto.class);

                assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

                return Objects.requireNonNull(res.getBody()).getId();
        }

        private Long createStudent(Long schoolId, String name) {

                StudentCreateRequest req = new StudentCreateRequest();

                req.setFirstName(name);
                req.setDob(LocalDate.of(2015, 1, 1));
                req.setGender(MALE);
                req.setAdmissionNumber("name" + System.currentTimeMillis());
                req.setGuardians(List.of(GuardianCreateRequest.builder()
                                .name("Promotion Guardian")
                                .contactNumber("7766554433")
                                .relation("FATHER")
                                .primaryGuardian(true)
                                .whatsappEnabled(Boolean.TRUE).build()));

                HttpEntity<StudentCreateRequest> entity = new HttpEntity<>(req, headers);

                ResponseEntity<StudentDto> res = restTemplate.exchange(
                                "/api/students",
                                HttpMethod.POST,
                                entity,
                                StudentDto.class);

                assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

                return Objects.requireNonNull(res.getBody()).getId();
        }

        private void enrollStudent(Long studentId,
                        Long classId,
                        String section,
                        Long sessionId) {

                StudentEnrollmentRequest req = new StudentEnrollmentRequest();

                req.setStudentId(studentId);
                req.setClassId(classId);
                req.setSection(section);
                req.setSessionId(sessionId);

                HttpEntity<StudentEnrollmentRequest> entity = new HttpEntity<>(req, headers);

                ResponseEntity<StudentEnrollmentDto> res = restTemplate.exchange(
                                "/api/enrollments",
                                HttpMethod.POST,
                                entity,
                                StudentEnrollmentDto.class);

                assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @AfterEach
        void cleanup() {
                fullCleanup();
        }

}
