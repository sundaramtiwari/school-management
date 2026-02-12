package com.school.backend.core.student.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.SessionResolver;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.core.student.dto.StudentUpdateRequest;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.mapper.StudentMapper;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository repository;
    private final StudentMapper mapper;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository classRepository;
    private final SessionResolver sessionResolver;

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

    @Transactional
    public StudentDto register(StudentCreateRequest req) {
        Long schoolId = TenantContext.getSchoolId();
        // duplicate check
        if (repository.existsByAdmissionNumberAndSchoolId(req.getAdmissionNumber(), schoolId)) {
            throw new IllegalArgumentException("Admission number already exists for this school");
        }

        Student ent = mapper.toEntity(req);
        Student saved = repository.save(ent);
        return mapper.toDto(saved);
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
        return mapper.toDto(s);
    }

    @Transactional(readOnly = true)
    public Page<StudentDto> listBySchool(Long schoolId, Pageable pageable) {
        Long sessionId = sessionResolver.resolveForCurrentSchool();
        return repository.findBySchoolIdAndSessionId(schoolId, sessionId, pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<StudentDto> listAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional
    public StudentDto update(Long id, StudentUpdateRequest req) {

        Student existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));

        updateStudentDetails(req, existing);

        return mapper.toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id))
            throw new ResourceNotFoundException("Student not found: " + id);
        // soft delete: mark active=false
        Student s = repository.findById(id).get();
        s.setActive(false);
        repository.save(s);
    }
}
