package com.school.backend.transport.service;

import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.SessionContext;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
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

    /**
     * Enrolls a student in transport for a session.
     *
     * @param dto Transport enrollment details
     * @return Enrollment DTO with assigned details
     * @throws ResourceNotFoundException if student or pickup point not found
     * @throws IllegalStateException     if student not in class or route at
     *                                   capacity
     */
    @Transactional
    public TransportEnrollmentDto enrollStudent(TransportEnrollmentDto dto) {
        Long schoolId = TenantContext.getSchoolId();
        Long effectiveSessionId = requireSessionId();
        if (dto.getSessionId() != null && !dto.getSessionId().equals(effectiveSessionId)) {
            throw new InvalidOperationException("Session mismatch between request and context");
        }
        dto.setSessionId(effectiveSessionId);
        log.info("Enrolling student {} in transport for session {} [Tenant: {}]",
                dto.getStudentId(), dto.getSessionId(), schoolId);

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
                .findByStudentIdAndSessionIdAndSchoolId(dto.getStudentId(), dto.getSessionId(), schoolId)
                .orElse(null);

        if (enrollment == null) {
            // NEW ENROLLMENT
            log.debug("New enrollment - checking capacity for route {}", route.getId());

            // Assign fee FIRST (so it can fail before we increment capacity)
            ensureFeeAssigned(student, pickupPoint, dto.getSessionId());

            // ✅ Atomic capacity check and increment (Tenant enforced)
            int updated = routeRepository.incrementStrengthIfCapacityAvailable(route.getId(), schoolId);

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
                            "Failed to enroll student. Route may be inactive or does not belong to your school.");
                }
            }

            // Create new enrollment
            enrollment = new TransportEnrollment();
            enrollment.setStudentId(dto.getStudentId());
            enrollment.setPickupPoint(pickupPoint);
            enrollment.setSessionId(dto.getSessionId());
            enrollment.setActive(true);
            enrollment.setSchoolId(schoolId);

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
                int updated = routeRepository.incrementStrengthIfCapacityAvailable(newRouteId, schoolId);

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
                    routeRepository.decrementStrength(oldRouteId, schoolId);

                    // ✅ Atomic: Increment new route (with capacity check)
                    int updated = routeRepository.incrementStrengthIfCapacityAvailable(newRouteId, schoolId);

                    if (updated == 0) {
                        // New route at capacity - rollback old route decrement
                        log.warn("Route {} at capacity - rolling back route change", newRouteId);
                        routeRepository.incrementStrengthIfCapacityAvailable(oldRouteId, schoolId);

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
     * Follows strict transactional ordering:
     * 1. Soft-delete enrollment (active = false)
     * 2. Decrement route strength
     * 3. Validate rows updated to ensure invariant safety
     *
     * @param studentId Student ID
     * @throws ResourceNotFoundException if active enrollment not found
     * @throws IllegalStateException     if invariant violation detected
     */
    @Transactional
    public void unenrollStudent(Long studentId) {
        unenrollStudent(studentId, requireSessionId());
    }

    @Transactional
    public void unenrollStudent(Long studentId, Long sessionId) {
        Long schoolId = TenantContext.getSchoolId();
        Long effectiveSessionId = sessionId != null ? sessionId : requireSessionId();
        log.info("Unenrolling student {} from transport for session {} [Tenant: {}]",
                studentId, effectiveSessionId, schoolId);

        TransportEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndSessionIdAndSchoolId(studentId, effectiveSessionId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No transport enrollment found for student " + studentId));

        if (!enrollment.isActive()) {
            log.warn("Attempted to unenroll already inactive enrollment for student {}", studentId);
            throw new IllegalStateException("Student is already unenrolled from transport");
        }

        // 1. Soft delete enrollment first (Atomic operation to prevent races)
        int enrollmentUpdated = enrollmentRepository.deactivateEnrollment(enrollment.getId(), schoolId);
        if (enrollmentUpdated == 0) {
            log.error("Failed to deactivate enrollment {} - already inactive or tenant mismatch", enrollment.getId());
            throw new IllegalStateException("Student is already unenrolled or enrollment state changed.");
        }

        // 2. Decrement route strength atomically
        Long routeId = enrollment.getPickupPoint().getRoute().getId();
        int routeUpdated = routeRepository.decrementStrength(routeId, schoolId);

        if (routeUpdated == 0) {
            log.error(
                    "Route strength invariant violation! Route {} strength could not be decremented for student unenrollment {}",
                    routeId, studentId);
            // This will trigger transaction rollback, restoring the enrollment 'active'
            // state
            throw new IllegalStateException("Route strength inconsistency detected. Data may be out of sync.");
        }

        // 3. Deactivate transport fee assignment
        deactivateTransportFeeAssignment(studentId, effectiveSessionId, enrollment.getPickupPoint());

        log.info("Student {} successfully unenrolled from transport. Capacity restored for route {}",
                studentId, routeId);
    }

    /**
     * Retrieves active transport enrollments for a list of student IDs.
     * Used for bulk status fetching in UI to avoid N+1 queries.
     *
     * @param studentIds List of student IDs
     * @return List of active enrollment DTOs
     */
    @Transactional(readOnly = true)
    public List<TransportEnrollmentDto> getActiveEnrollmentsForStudents(Collection<Long> studentIds) {
        return getActiveEnrollmentsForStudents(studentIds, requireSessionId());
    }

    @Transactional(readOnly = true)
    public List<TransportEnrollmentDto> getActiveEnrollmentsForStudents(Collection<Long> studentIds,
                                                                        Long sessionId) {
        Long effectiveSessionId = sessionId != null ? sessionId : requireSessionId();
        return enrollmentRepository.findByStudentIdInAndSessionIdAndActiveTrue(studentIds, effectiveSessionId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Deactivates transport fee assignment for a student.
     * Called when student is unenrolled from transport.
     *
     * @param studentId Student ID
     * @param sessionId Session ID
     */
    private void deactivateTransportFeeAssignment(Long studentId, Long sessionId, PickupPoint pickupPoint) {
        Long schoolId = TenantContext.getSchoolId();
        FeeType transportType = getOrCreateTransportFeeType(schoolId);

        feeStructureRepository.findByFeeTypeIdAndSessionIdAndClassIdIsNullAndSchoolIdAndAmountAndFrequency(
                transportType.getId(), sessionId, schoolId, pickupPoint.getAmount(), pickupPoint.getFrequency()).flatMap(structure -> assignmentRepository
                .findByStudentIdAndFeeStructureIdAndSessionIdAndSchoolId(
                        studentId, structure.getId(), sessionId, schoolId)).ifPresent(assignment -> {
            assignment.setActive(false);
            assignmentRepository.save(assignment);
            log.debug("Deactivated transport fee assignment for student {}", studentId);
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

        Long schoolId = TenantContext.getSchoolId();

        // 1. Ensure TRANSPORT FeeType exists
        FeeType transportFeeType = getOrCreateTransportFeeType(schoolId);

        // 2. Find or create fee structure for this pickup point's amount and frequency
        FeeStructure structure = feeStructureRepository
                .findByFeeTypeIdAndSessionIdAndClassIdIsNullAndSchoolIdAndAmountAndFrequency(
                        transportFeeType.getId(), sessionId, schoolId, pp.getAmount(), pp.getFrequency())
                .orElseGet(() -> {
                    log.debug(
                            "Creating new transport fee structure for pickup point {} with amount {} and frequency {}",
                            pp.getId(), pp.getAmount(), pp.getFrequency());
                    FeeStructure fs = FeeStructure.builder()
                            .feeType(transportFeeType)
                            .sessionId(sessionId)
                            .amount(pp.getAmount())
                            .frequency(pp.getFrequency())
                            .classId(null) // Global fee (not class-specific)
                            .active(true)
                            .schoolId(schoolId)
                            .build();
                    return feeStructureRepository.saveAndFlush(fs);
                });

        // Check if already assigned (and active)
        Optional<StudentFeeAssignment> existingAssignment = assignmentRepository
                .findByStudentIdAndFeeStructureIdAndSessionIdAndSchoolId(
                        student.getId(), structure.getId(), sessionId, schoolId);

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
            BigDecimal finalAmount = structure.getAmount()
                    .multiply(java.math.BigDecimal.valueOf(structure.getFrequency().getPeriodsPerYear()));

            StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                    .studentId(student.getId())
                    .feeStructureId(structure.getId())
                    .sessionId(sessionId)
                    .amount(finalAmount)
                    .active(true)
                    .schoolId(schoolId)
                    .build();
            assignmentRepository.save(assignment);

            log.info("Assigned transport fee of ₹{} to student {}", finalAmount, student.getId());
        }
    }

    /**
     * Gets or creates the TRANSPORT fee type for a school.
     *
     * @return Transport FeeType entity
     */
    private FeeType getOrCreateTransportFeeType(Long schoolId) {
        return feeTypeRepository.findByNameAndSchoolId("TRANSPORT", schoolId)
                .orElseGet(() -> {
                    log.info("Creating TRANSPORT fee type for school {}", schoolId);
                    try {
                        return feeTypeRepository.saveAndFlush(FeeType.builder()
                                .name("TRANSPORT")
                                .description("Transport / Bus Fees")
                                .active(true)
                                .schoolId(schoolId)
                                .build());
                    } catch (DataIntegrityViolationException ex) {
                        // Unique constraint race: another transaction inserted same fee type.
                        return feeTypeRepository.findByNameAndSchoolId("TRANSPORT", schoolId)
                                .orElseThrow(() -> ex);
                    }
                });
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
                .pickupPointName(e.getPickupPoint().getName())
                .routeId(e.getPickupPoint().getRoute().getId())
                .routeName(e.getPickupPoint().getRoute().getName())
                .sessionId(e.getSessionId())
                .active(e.isActive())
                .build();
    }

    /**
     * Retrieves a student's transport enrollment for a session.
     *
     * @param studentId Student ID
     * @return Optional containing enrollment DTO if exists
     */
    @Transactional(readOnly = true)
    public Optional<TransportEnrollmentDto> getStudentEnrollment(Long studentId) {
        return getStudentEnrollment(studentId, requireSessionId());
    }

    @Transactional(readOnly = true)
    public Optional<TransportEnrollmentDto> getStudentEnrollment(Long studentId, Long sessionId) {
        Long schoolId = TenantContext.getSchoolId();
        Long effectiveSessionId = sessionId != null ? sessionId : requireSessionId();
        log.debug("Fetching transport enrollment for student {} in session {} [Tenant: {}]",
                studentId, effectiveSessionId, schoolId);
        return enrollmentRepository.findByStudentIdAndSessionIdAndSchoolId(studentId, effectiveSessionId, schoolId)
                .map(this::mapToDto);
    }

    private Long requireSessionId() {
        Long sessionId = SessionContext.getSessionId();
        if (sessionId == null) {
            throw new InvalidOperationException("Session context is missing in request");
        }
        return sessionId;
    }
}
