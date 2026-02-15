package com.school.backend.transport.service;

import com.school.backend.common.tenant.TenantContext;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.enums.FeeFrequency;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.transport.dto.TransportEnrollmentDto;
import com.school.backend.transport.entity.PickupPoint;
import com.school.backend.transport.entity.TransportEnrollment;
import com.school.backend.transport.entity.TransportRoute;
import com.school.backend.transport.repository.PickupPointRepository;
import com.school.backend.transport.repository.TransportEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransportEnrollmentService {

        private final TransportEnrollmentRepository enrollmentRepository;
        private final PickupPointRepository pickupPointRepository;
        private final StudentRepository studentRepository;
        private final FeeTypeRepository feeTypeRepository;
        private final FeeStructureRepository feeStructureRepository;
        private final StudentFeeAssignmentRepository assignmentRepository;

        @Transactional
        public TransportEnrollmentDto enrollStudent(TransportEnrollmentDto dto) {
                // 1. Validate Student & Class Enrollment
                Student student = studentRepository.findById(dto.getStudentId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Student not found: " + dto.getStudentId()));

                if (student.getCurrentClass() == null) {
                        throw new IllegalStateException(
                                        "Student must be assigned to a class before transport enrollment.");
                }

                PickupPoint pickupPoint = pickupPointRepository.findById(dto.getPickupPointId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Pickup point not found: " + dto.getPickupPointId()));

                TransportRoute route = pickupPoint.getRoute();

                // 2. Create/Update Enrollment
                TransportEnrollment enrollment = enrollmentRepository
                                .findByStudentIdAndSessionId(dto.getStudentId(), dto.getSessionId())
                                .orElse(null);

                if (enrollment == null) {
                        // New Enrollment - check capacity
                        if (route.getCurrentStrength() >= route.getCapacity()) {
                                throw new IllegalStateException(
                                                "Transport route " + route.getName() + " is at full capacity.");
                        }
                        enrollment = new TransportEnrollment();
                        route.setCurrentStrength(route.getCurrentStrength() + 1);
                } else {
                        // Moving to a different point/route
                        if (!enrollment.getPickupPoint().getRoute().getId().equals(route.getId())) {
                                // Decrement old route, check/increment new route
                                TransportRoute oldRoute = enrollment.getPickupPoint().getRoute();
                                oldRoute.setCurrentStrength(Math.max(0, oldRoute.getCurrentStrength() - 1));

                                if (route.getCurrentStrength() >= route.getCapacity()) {
                                        throw new IllegalStateException(
                                                        "Transport route " + route.getName() + " is at full capacity.");
                                }
                                route.setCurrentStrength(route.getCurrentStrength() + 1);
                        }
                }

                enrollment.setStudentId(dto.getStudentId());
                enrollment.setPickupPoint(pickupPoint);
                enrollment.setSessionId(dto.getSessionId());
                enrollment.setActive(true);
                enrollment.setSchoolId(TenantContext.getSchoolId());
                enrollment = enrollmentRepository.save(enrollment);

                // 3. Fee Integration
                ensureFeeAssigned(student, pickupPoint, dto.getSessionId());

                return mapToDto(enrollment);
        }

        private void ensureFeeAssigned(Student student, PickupPoint pp, Long sessionId) {
                // Get or Create TRANSPORT FeeType
                FeeType transportType = getOrCreateTransportFeeType();

                // Get or Create FeeStructure for this PickupPoint
                // We use the pickupPointId in a hidden way or just name match?
                // Better to use a specific convention: "Transport - [RouteName] - [PointName]"

                // Find existing structure for this specific pickup point in this session
                // Since we refactored classId to be nullable, we can find a structure where
                // classId IS NULL
                FeeStructure structure = feeStructureRepository
                                .findByFeeTypeIdAndSessionIdAndClassIdIsNull(transportType.getId(), sessionId)
                                .stream()
                                .filter(fs -> fs.getAmount().equals(pp.getAmount())
                                                && fs.getFrequency().equals(pp.getFrequency()))
                                .findFirst()
                                .orElseGet(() -> {
                                        FeeStructure fs = FeeStructure.builder()
                                                        .feeType(transportType)
                                                        .sessionId(sessionId)
                                                        .amount(pp.getAmount())
                                                        .frequency(pp.getFrequency())
                                                        .classId(null) // Global fee
                                                        .active(true)
                                                        .schoolId(TenantContext.getSchoolId())
                                                        .build();
                                        return feeStructureRepository.save(fs);
                                });

                // Assign to student if not already assigned
                boolean alreadyAssigned = assignmentRepository.existsByStudentIdAndFeeStructureIdAndSessionId(
                                student.getId(), structure.getId(), sessionId);

                if (!alreadyAssigned) {
                        int finalAmount = structure.getAmount();
                        if (structure.getFrequency() == FeeFrequency.MONTHLY) {
                                finalAmount = finalAmount * 12;
                        }
                        StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                                        .studentId(student.getId())
                                        .feeStructureId(structure.getId())
                                        .sessionId(sessionId)
                                        .amount(finalAmount)
                                        .active(true)
                                        .schoolId(TenantContext.getSchoolId())
                                        .build();
                        assignmentRepository.save(assignment);
                }
        }

        private FeeType getOrCreateTransportFeeType() {
                return feeTypeRepository.findAll().stream()
                                .filter(t -> t.getName().equalsIgnoreCase("TRANSPORT"))
                                .findFirst()
                                .orElseGet(() -> {
                                        FeeType t = FeeType.builder()
                                                        .name("TRANSPORT")
                                                        .description("Transport / Bus Fees")
                                                        .active(true)
                                                        .schoolId(TenantContext.getSchoolId())
                                                        .build();
                                        return feeTypeRepository.save(t);
                                });
        }

        private TransportEnrollmentDto mapToDto(TransportEnrollment e) {
                return TransportEnrollmentDto.builder()
                                .id(e.getId())
                                .studentId(e.getStudentId())
                                .pickupPointId(e.getPickupPoint().getId())
                                .sessionId(e.getSessionId())
                                .active(e.isActive())
                                .build();
        }

        @Transactional(readOnly = true)
        public java.util.Optional<TransportEnrollmentDto> getStudentEnrollment(Long studentId, Long sessionId) {
                return enrollmentRepository.findByStudentIdAndSessionId(studentId, sessionId)
                                .map(this::mapToDto);
        }
}
