package com.school.backend.transport.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.transport.dto.TransportEnrollmentDto;
import com.school.backend.transport.entity.PickupPoint;
import com.school.backend.transport.entity.TransportEnrollment;
import com.school.backend.transport.entity.TransportRoute;
import com.school.backend.transport.repository.PickupPointRepository;
import com.school.backend.transport.repository.TransportEnrollmentRepository;
import com.school.backend.transport.repository.TransportRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing student transport enrollments.
 * <p>
 * Key Features:
 * - Atomic capacity management (prevents overbooking)
 * - Automatic fee assignment integration
 * - Support for route changes
 * - Soft delete for enrollment history
 *
 * @author Claude
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransportEnrollmentService {

    private final TransportEnrollmentRepository enrollmentRepository;
    private final TransportRouteRepository routeRepository;
    private final PickupPointRepository pickupPointRepository;
    private final StudentRepository studentRepository;
    private final FeeTypeRepository feeTypeRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;

    // Cache for transport fee type to avoid repeated DB queries
    private FeeType transportFeeTypeCache;

    /**
     * Enrolls a student in transport for a session.
     *
     * @param dto Transport enrollment details
     * @return Enrollment DTO with assigned details
     * @throws ResourceNotFoundException if student or pickup point not found
     * @throws IllegalStateException     if student not in class or route at capacity
     */
    @Transactional
    public TransportEnrollmentDto enrollStudent(TransportEnrollmentDto dto) {
        log.info("Enrolling student {} in transport for session {}", dto.getStudentId(), dto.getSessionId());

        // 1. Validate Student & Class Enrollment
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found: " + dto.getStudentId()));

        if (student.getCurrentClass() == null) {
            log.warn("Attempted to enroll student {} without class assignment", dto.getStudentId());
            throw new IllegalStateException(
                    "Student must be assigned to a class before transport enrollment.");
        }

        PickupPoint pickupPoint = pickupPointRepository.findById(dto.getPickupPointId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pickup point not found: " + dto.getPickupPointId()));

        TransportRoute route = pickupPoint.getRoute();

        // 2. Check for existing enrollment
        TransportEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndSessionId(dto.getStudentId(), dto.getSessionId())
                .orElse(null);

        if (enrollment == null) {
            // NEW ENROLLMENT
            log.debug("New enrollment - checking capacity for route {}", route.getId());

            // Assign fee FIRST (so it can fail before we increment capacity)
            ensureFeeAssigned(student, pickupPoint, dto.getSessionId());

            // ✅ Atomic capacity check and increment
            int updated = routeRepository.incrementStrengthIfCapacityAvailable(route.getId());

            if (updated == 0) {
                log.warn("Route {} at capacity or inactive - enrollment rejected", route.getId());

                // Re-load to provide specific error message
                TransportRoute freshRoute = routeRepository.findById(route.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

                if (freshRoute.getCurrentStrength() >= freshRoute.getCapacity()) {
                    throw new IllegalStateException(
                            "Transport route " + freshRoute.getName() +
                                    " is at full capacity (" + freshRoute.getCapacity() + " seats).");
                } else {
                    throw new IllegalStateException(
                            "Failed to enroll student. Route may be inactive.");
                }
            }

            // Create new enrollment
            enrollment = new TransportEnrollment();
            enrollment.setStudentId(dto.getStudentId());
            enrollment.setPickupPoint(pickupPoint);
            enrollment.setSessionId(dto.getSessionId());
            enrollment.setActive(true);
            enrollment.setSchoolId(TenantContext.getSchoolId());

            log.info("Student {} enrolled in route {} successfully", dto.getStudentId(), route.getId());

        } else {
            // UPDATING EXISTING ENROLLMENT
            log.debug("Updating existing enrollment for student {}", dto.getStudentId());

            Long oldRouteId = enrollment.getPickupPoint().getRoute().getId();
            Long newRouteId = route.getId();

            // Check if this is a re-enrollment after being inactive
            boolean wasInactive = !enrollment.isActive();

            if (wasInactive) {
                log.debug("Re-enrolling previously inactive student {}", dto.getStudentId());

                // Re-assign fee (in case it was removed)
                ensureFeeAssigned(student, pickupPoint, dto.getSessionId());

                // Need to increment capacity again
                int updated = routeRepository.incrementStrengthIfCapacityAvailable(newRouteId);

                if (updated == 0) {
                    log.warn("Route {} at capacity - re-enrollment rejected", newRouteId);
                    throw new IllegalStateException(
                            "Transport route " + route.getName() + " is at full capacity.");
                }

                enrollment.setActive(true);
            }

            if (!oldRouteId.equals(newRouteId)) {
                // Moving to different route
                log.debug("Moving student {} from route {} to route {}",
                        dto.getStudentId(), oldRouteId, newRouteId);

                if (!wasInactive) {
                    // Only decrement old route if was active
                    // ✅ Atomic: Decrement old route
                    routeRepository.decrementStrength(oldRouteId);

                    // ✅ Atomic: Increment new route (with capacity check)
                    int updated = routeRepository.incrementStrengthIfCapacityAvailable(newRouteId);

                    if (updated == 0) {
                        // New route at capacity - rollback old route decrement
                        log.warn("Route {} at capacity - rolling back route change", newRouteId);
                        routeRepository.incrementStrengthIfCapacityAvailable(oldRouteId);

                        throw new IllegalStateException(
                                "Transport route " + route.getName() + " is at full capacity.");
                    }
                }

                // Update fee assignment for new pickup point
                ensureFeeAssigned(student, pickupPoint, dto.getSessionId());
            }

            // Update pickup point (even if same route, might be different stop)
            enrollment.setPickupPoint(pickupPoint);
        }

        // 3. Save enrollment
        enrollment = enrollmentRepository.save(enrollment);

        log.info("Transport enrollment completed for student {}", dto.getStudentId());
        return mapToDto(enrollment);
    }

    /**
     * Unenrolls a student from transport.
     * Decrements route capacity and soft-deletes the enrollment.
     *
     * @param studentId Student ID
     * @param sessionId Session ID
     * @throws ResourceNotFoundException if enrollment not found
     */
    @Transactional
    public void unenrollStudent(Long studentId, Long sessionId) {
        log.info("Unenrolling student {} from transport for session {}", studentId, sessionId);

        TransportEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndSessionId(studentId, sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No transport enrollment found for student " + studentId));

        if (!enrollment.isActive()) {
            log.warn("Attempted to unenroll already inactive enrollment for student {}", studentId);
            throw new IllegalStateException("Student is already unenrolled from transport");
        }

        // Decrement route strength
        Long routeId = enrollment.getPickupPoint().getRoute().getId();
        routeRepository.decrementStrength(routeId);

        // Soft delete enrollment
        enrollment.setActive(false);
        enrollmentRepository.save(enrollment);

        // Optionally deactivate transport fee assignment
        // Note: We soft-delete instead of hard-delete to preserve payment history
        deactivateTransportFeeAssignment(studentId, sessionId);

        log.info("Student {} successfully unenrolled from transport", studentId);
    }

    /**
     * Deactivates transport fee assignment for a student.
     * Called when student is unenrolled from transport.
     *
     * @param studentId Student ID
     * @param sessionId Session ID
     */
    private void deactivateTransportFeeAssignment(Long studentId, Long sessionId) {
        FeeType transportType = getOrCreateTransportFeeType();

        // Find all transport fee structures for this session
        feeStructureRepository
                .findByFeeTypeIdAndSessionIdAndClassIdIsNull(transportType.getId(), sessionId)
                .forEach(structure -> {
                    // Find and deactivate student's assignment to this structure
                    assignmentRepository
                            .findByStudentIdAndFeeStructureIdAndSessionId(studentId, structure.getId(), sessionId)
                            .ifPresent(assignment -> {
                                assignment.setActive(false);
                                assignmentRepository.save(assignment);
                                log.debug("Deactivated transport fee assignment for student {}", studentId);
                            });
                });
    }

    /**
     * Ensures transport fee is assigned to student for the given pickup point.
     * Creates fee structure if needed and assigns to student.
     *
     * @param student   Student entity
     * @param pp        Pickup point
     * @param sessionId Session ID
     */
    private void ensureFeeAssigned(Student student, PickupPoint pp, Long sessionId) {
        log.debug("Ensuring transport fee assigned for student {}", student.getId());

        // Get or Create TRANSPORT FeeType
        FeeType transportType = getOrCreateTransportFeeType();

        // Find or create fee structure for this pickup point
        FeeStructure structure = feeStructureRepository
                .findByFeeTypeIdAndSessionIdAndClassIdIsNull(transportType.getId(), sessionId)
                .stream()
                .filter(fs -> fs.getAmount().equals(pp.getAmount())
                        && fs.getFrequency().equals(pp.getFrequency()))
                .findFirst()
                .orElseGet(() -> {
                    log.debug("Creating new transport fee structure for pickup point {}", pp.getId());
                    FeeStructure fs = FeeStructure.builder()
                            .feeType(transportType)
                            .sessionId(sessionId)
                            .amount(pp.getAmount())
                            .frequency(pp.getFrequency())
                            .classId(null) // Global fee (not class-specific)
                            .active(true)
                            .schoolId(TenantContext.getSchoolId())
                            .build();
                    return feeStructureRepository.save(fs);
                });

        // Check if already assigned (and active)
        Optional<StudentFeeAssignment> existingAssignment = assignmentRepository
                .findByStudentIdAndFeeStructureIdAndSessionId(student.getId(), structure.getId(), sessionId);

        if (existingAssignment.isPresent()) {
            StudentFeeAssignment assignment = existingAssignment.get();
            if (!assignment.isActive()) {
                // Reactivate if was deactivated
                assignment.setActive(true);
                assignmentRepository.save(assignment);
                log.debug("Reactivated transport fee assignment for student {}", student.getId());
            } else {
                log.debug("Transport fee already assigned to student {}", student.getId());
            }
        } else {
            // Create new assignment
            int finalAmount = structure.getAmount() * structure.getFrequency().getPeriodsPerYear();

            StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                    .studentId(student.getId())
                    .feeStructureId(structure.getId())
                    .sessionId(sessionId)
                    .amount(finalAmount)
                    .active(true)
                    .schoolId(TenantContext.getSchoolId())
                    .build();
            assignmentRepository.save(assignment);

            log.info("Assigned transport fee of ₹{} to student {}", finalAmount, student.getId());
        }
    }

    /**
     * Gets or creates the TRANSPORT fee type.
     * Uses caching to avoid repeated database queries.
     *
     * @return Transport FeeType entity
     */
    private synchronized FeeType getOrCreateTransportFeeType() {
        if (transportFeeTypeCache == null) {
            log.debug("Loading TRANSPORT fee type");

            transportFeeTypeCache = feeTypeRepository.findAll().stream()
                    .filter(t -> t.getName().equalsIgnoreCase("TRANSPORT"))
                    .findFirst()
                    .orElseGet(() -> {
                        log.info("Creating TRANSPORT fee type");
                        FeeType t = FeeType.builder()
                                .name("TRANSPORT")
                                .description("Transport / Bus Fees")
                                .active(true)
                                .schoolId(TenantContext.getSchoolId())
                                .build();
                        return feeTypeRepository.save(t);
                    });
        }
        return transportFeeTypeCache;
    }

    /**
     * Maps TransportEnrollment entity to DTO.
     *
     * @param e TransportEnrollment entity
     * @return TransportEnrollmentDto
     */
    private TransportEnrollmentDto mapToDto(TransportEnrollment e) {
        return TransportEnrollmentDto.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .pickupPointId(e.getPickupPoint().getId())
                .sessionId(e.getSessionId())
                .active(e.isActive())
                .build();
    }

    /**
     * Retrieves a student's transport enrollment for a session.
     *
     * @param studentId Student ID
     * @param sessionId Session ID
     * @return Optional containing enrollment DTO if exists
     */
    @Transactional(readOnly = true)
    public Optional<TransportEnrollmentDto> getStudentEnrollment(Long studentId, Long sessionId) {
        log.debug("Fetching transport enrollment for student {} in session {}", studentId, sessionId);
        return enrollmentRepository.findByStudentIdAndSessionId(studentId, sessionId)
                .map(this::mapToDto);
    }
}