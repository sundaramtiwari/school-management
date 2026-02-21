package com.school.backend.core.student.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.SessionResolver;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.guardian.entity.Guardian;
import com.school.backend.core.guardian.service.GuardianService;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.core.student.dto.StudentGuardianDto;
import com.school.backend.core.student.dto.StudentUpdateRequest;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.entity.StudentGuardian;
import com.school.backend.core.student.mapper.StudentMapper;
import com.school.backend.core.student.repository.StudentGuardianRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.school.service.SetupValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository repository;
    private final StudentMapper mapper;
    private final SessionResolver sessionResolver;
    private final SetupValidationService setupValidationService;
    private final GuardianService guardianService;
    private final StudentGuardianRepository studentGuardianRepository;

    private static void updateStudentDetails(StudentUpdateRequest req, Student existing) {
        if (req.getFirstName() != null)
            existing.setFirstName(req.getFirstName());

        if (req.getLastName() != null)
            existing.setLastName(req.getLastName());

        if (req.getDob() != null)
            existing.setDob(req.getDob());

        if (req.getGender() != null)
            existing.setGender(req.getGender());

        if (req.getPen() != null)
            existing.setPen(req.getPen());

        if (req.getAadharNumber() != null)
            existing.setAadharNumber(req.getAadharNumber());

        if (req.getReligion() != null)
            existing.setReligion(req.getReligion());

        if (req.getCaste() != null)
            existing.setCaste(req.getCaste());

        if (req.getCategory() != null)
            existing.setCategory(req.getCategory());

        if (req.getAddress() != null)
            existing.setAddress(req.getAddress());

        if (req.getCity() != null)
            existing.setCity(req.getCity());

        if (req.getState() != null)
            existing.setState(req.getState());

        if (req.getPincode() != null)
            existing.setPincode(req.getPincode());

        if (req.getContactNumber() != null)
            existing.setContactNumber(req.getContactNumber());

        if (req.getEmail() != null)
            existing.setEmail(req.getEmail());

        if (req.getBloodGroup() != null)
            existing.setBloodGroup(req.getBloodGroup());

        if (req.getPhotoUrl() != null)
            existing.setPhotoUrl(req.getPhotoUrl());

        if (req.getDateOfLeaving() != null)
            existing.setDateOfLeaving(req.getDateOfLeaving());

        if (req.getReasonForLeaving() != null)
            existing.setReasonForLeaving(req.getReasonForLeaving());

        if (req.getRemarks() != null)
            existing.setRemarks(req.getRemarks());

        // Previous School
        if (req.getPreviousSchoolName() != null)
            existing.setPreviousSchoolName(req.getPreviousSchoolName());

        if (req.getPreviousSchoolContact() != null)
            existing.setPreviousSchoolContact(req.getPreviousSchoolContact());

        if (req.getPreviousSchoolAddress() != null)
            existing.setPreviousSchoolAddress(req.getPreviousSchoolAddress());

        if (req.getPreviousSchoolBoard() != null)
            existing.setPreviousSchoolBoard(req.getPreviousSchoolBoard());

        if (req.getPreviousClass() != null)
            existing.setPreviousClass(req.getPreviousClass());

        if (req.getPreviousYearOfPassing() != null)
            existing.setPreviousYearOfPassing(req.getPreviousYearOfPassing());

        if (req.getTransferCertificateNumber() != null)
            existing.setTransferCertificateNumber(req.getTransferCertificateNumber());

        if (req.getReasonForLeavingPreviousSchool() != null)
            existing.setReasonForLeavingPreviousSchool(req.getReasonForLeavingPreviousSchool());

        if (req.getActive() != null)
            existing.setActive(req.getActive());

        if (req.getCurrentStatus() != null)
            existing.setCurrentStatus(req.getCurrentStatus());
    }

    private void validateGuardians(List<com.school.backend.core.guardian.dto.GuardianCreateRequest> guardians) {
        if (guardians == null || guardians.isEmpty()) {
            throw new IllegalArgumentException("At least one guardian is required");
        }

        long primaryCount = guardians.stream().filter(GuardianCreateRequest::isPrimaryGuardian).count();
        if (primaryCount > 1) {
            throw new IllegalArgumentException("Only one primary guardian is allowed");
        }
    }

    private void validateExactlyOnePrimaryGuardian(List<GuardianCreateRequest> guardians) {
        long primaryCount = guardians.stream()
                .filter(GuardianCreateRequest::isPrimaryGuardian)
                .count();
        if (primaryCount == 0) {
            throw new IllegalArgumentException("Exactly one primary guardian is required");
        }
        if (primaryCount > 1) {
            throw new IllegalArgumentException("Only one primary guardian is allowed");
        }
    }

    private void linkGuardians(Long studentId, Long schoolId,
            List<com.school.backend.core.guardian.dto.GuardianCreateRequest> guardians) {
        long primaryCount = guardians.stream().filter(GuardianCreateRequest::isPrimaryGuardian).count();

        for (int i = 0; i < guardians.size(); i++) {
            var gReq = guardians.get(i);
            Guardian g = guardianService.findOrCreateByContact(schoolId, gReq);

            boolean isPrimary = (primaryCount == 0 && i == 0) || gReq.isPrimaryGuardian();

            StudentGuardian sg = StudentGuardian.builder()
                    .studentId(studentId)
                    .guardianId(g.getId())
                    .primaryGuardian(isPrimary)
                    .build();
            sg.setSchoolId(schoolId);
            studentGuardianRepository.save(sg);
        }
    }

    @Transactional
    public StudentDto register(StudentCreateRequest req) {
        Long schoolId = TenantContext.getSchoolId();
        Long sessionId = sessionResolver.resolveForCurrentSchool();

        // Validate that at least one class exists
        setupValidationService.ensureAtLeastOneClassExists(schoolId, sessionId);

        // duplicate check
        if (repository.existsByAdmissionNumberAndSchoolId(req.getAdmissionNumber(), schoolId)) {
            throw new IllegalArgumentException("Admission number already exists for this school");
        }

        validateGuardians(req.getGuardians());

        Student ent = mapper.toEntity(req);
        Student saved = repository.save(ent);

        linkGuardians(saved.getId(), schoolId, req.getGuardians());

        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<StudentGuardianDto> getGuardiansForStudent(Long studentId) {
        Student student = repository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // Tenant Security Check
        if (!student.getSchoolId().equals(TenantContext.getSchoolId())) {
            throw new AccessDeniedException("Unauthorized access to student guardians");
        }

        return studentGuardianRepository.findByStudentId(studentId).stream()
                .map(sg -> {
                    StudentGuardianDto dto = new StudentGuardianDto();
                    dto.setId(sg.getId());
                    dto.setStudentId(sg.getStudentId());
                    dto.setGuardianId(sg.getGuardianId());
                    dto.setPrimaryGuardian(sg.isPrimaryGuardian());

                    guardianService.getOptionalById(sg.getGuardianId()).ifPresent(g -> {
                        dto.setName(g.getName());
                        dto.setRelation(g.getRelation());
                        dto.setContactNumber(g.getContactNumber());
                        dto.setEmail(g.getEmail());
                        dto.setAddress(g.getAddress());
                        dto.setAadharNumber(g.getAadharNumber());
                        dto.setOccupation(g.getOccupation());
                        dto.setQualification(g.getQualification());
                        dto.setWhatsappEnabled(g.isWhatsappEnabled());
                    });
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void replaceGuardians(Long studentId, List<GuardianCreateRequest> requests) {

        // ── 1. Validate input ──────────────────────────────────────────────────
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one guardian is required");
        }

        validateExactlyOnePrimaryGuardian(requests);

        // ── 2. Tenant + ownership check ────────────────────────────────────────
        Long schoolId = TenantContext.getSchoolId();

        Student student = repository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        if (!student.getSchoolId().equals(schoolId)) {
            throw new AccessDeniedException("Unauthorized access to student guardians");
        }

        // ── 3. Load existing mappings once ─────────────────────────────────────
        List<StudentGuardian> existingMappings = studentGuardianRepository.findByStudentId(studentId);

        Map<Long, StudentGuardian> existingByGuardianId = existingMappings.stream()
                .collect(Collectors.toMap(StudentGuardian::getGuardianId, sg -> sg));

        // ── 4. Upsert incoming guardians ───────────────────────────────────────
        Set<Long> incomingGuardianIds = new HashSet<>();

        for (GuardianCreateRequest req : requests) {
            Guardian guardian = guardianService.findOrCreateByContactWithoutMutatingExisting(schoolId, req);
            Long guardianId = guardian.getId();
            incomingGuardianIds.add(guardianId);

            if (existingByGuardianId.containsKey(guardianId)) {
                // Update primary flag and save explicitly
                StudentGuardian sg = existingByGuardianId.get(guardianId);
                sg.setPrimaryGuardian(req.isPrimaryGuardian());
                studentGuardianRepository.save(sg); // ← was missing before
            } else {
                // New mapping
                StudentGuardian sg = new StudentGuardian();
                sg.setStudentId(studentId);
                sg.setGuardianId(guardianId);
                sg.setPrimaryGuardian(req.isPrimaryGuardian());
                sg.setSchoolId(schoolId);
                studentGuardianRepository.save(sg);
            }
        }

        // ── 5. Batch delete removed guardians ─────────────────────────────────
        List<StudentGuardian> toDelete = existingMappings.stream()
                .filter(sg -> !incomingGuardianIds.contains(sg.getGuardianId()))
                .collect(Collectors.toList());

        if (!toDelete.isEmpty()) {
            studentGuardianRepository.deleteAll(toDelete); // single batch delete
        }

        // ── 6. Final integrity check ───────────────────────────────────────────
        // Guard against edge case where findOrCreateByContact merged two
        // incoming requests into the same guardian (same contact number,
        // different relation), which could result in zero or two primaries.
        List<StudentGuardian> finalMappings = studentGuardianRepository.findByStudentId(studentId);

        long finalPrimaryCount = finalMappings.stream()
                .filter(StudentGuardian::isPrimaryGuardian)
                .count();

        if (finalPrimaryCount != 1) {
            throw new IllegalStateException(
                    "Guardian replacement resulted in " + finalPrimaryCount +
                            " primary guardians. Expected exactly 1. Rolling back.");
        }
    }

    @Transactional(readOnly = true)
    public Page<StudentDto> listByClass(Long classId, Pageable pageable) {
        Long sessionId = sessionResolver.resolveForCurrentSchool();

        return repository
                .findByClassIdAndSessionId(classId, sessionId, pageable)
                .map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public StudentDto getById(Long id) {
        Student s = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));

        if (!s.getSchoolId().equals(TenantContext.getSchoolId())) {
            throw new AccessDeniedException("Unauthorized access to student");
        }
        return mapper.toDto(s);
    }

    @Transactional(readOnly = true)
    public Page<StudentDto> listBySchool(Long schoolId, Pageable pageable) {
        Long sessionId = sessionResolver.resolveForCurrentSchool();
        return repository.findBySchoolIdAndSessionId(schoolId, sessionId, pageable).map(mapper::toDto);
    }

    @Transactional
    public StudentDto update(Long id, StudentUpdateRequest req) {

        Student existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));

        if (!existing.getSchoolId().equals(TenantContext.getSchoolId())) {
            throw new AccessDeniedException("Unauthorized access to student");
        }

        updateStudentDetails(req, existing);

        return mapper.toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        Student s = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));

        if (!s.getSchoolId().equals(TenantContext.getSchoolId())) {
            throw new AccessDeniedException("Unauthorized access to student");
        }

        // Soft delete by marking as inactive
        s.setActive(false);
        repository.save(s);
    }
}
