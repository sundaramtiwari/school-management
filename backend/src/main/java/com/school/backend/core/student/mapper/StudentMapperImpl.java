package com.school.backend.core.student.mapper;

import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.core.student.entity.Student;
import org.springframework.stereotype.Component;

@Component
public class StudentMapperImpl implements StudentMapper {

    @Override
    public StudentDto toDto(Student entity) {
        return toDto(entity, false);
    }

    @Override
    public StudentDto toDto(Student entity, boolean enrollmentActive) {
        if (entity == null)
            return null;
        StudentDto dto = new StudentDto();
        dto.setId(entity.getId());
        dto.setAdmissionNumber(entity.getAdmissionNumber());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setDob(entity.getDob());
        dto.setGender(entity.getGender());
        dto.setPen(entity.getPen());
        dto.setAadharNumber(entity.getAadharNumber());
        dto.setReligion(entity.getReligion());
        dto.setCaste(entity.getCaste());
        dto.setCategory(entity.getCategory());
        dto.setAddress(entity.getAddress());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setPincode(entity.getPincode());
        dto.setContactNumber(entity.getContactNumber());
        dto.setEmail(entity.getEmail());
        dto.setBloodGroup(entity.getBloodGroup());
        dto.setPhotoUrl(entity.getPhotoUrl());
        dto.setDateOfAdmission(entity.getDateOfAdmission());
        dto.setDateOfLeaving(entity.getDateOfLeaving());
        dto.setReasonForLeaving(entity.getReasonForLeaving());
        dto.setActive(entity.isActive());
        dto.setCurrentStatus(entity.getCurrentStatus());
        if (entity.getCurrentClass() != null)
            dto.setCurrentClassId(entity.getCurrentClass().getId());
        if (entity.getSchool() != null)
            dto.setSchoolId(entity.getSchool().getId());
        dto.setRemarks(entity.getRemarks());
        dto.setPreviousSchoolName(entity.getPreviousSchoolName());
        dto.setPreviousSchoolBoard(entity.getPreviousSchoolBoard());
        dto.setPreviousClass(entity.getPreviousClass());
        dto.setPreviousYearOfPassing(entity.getPreviousYearOfPassing());
        dto.setTransferCertificateNumber(entity.getTransferCertificateNumber());
        dto.setPreviousSchoolAddress(entity.getPreviousSchoolAddress());
        dto.setPreviousSchoolContact(entity.getPreviousSchoolContact());
        dto.setReasonForLeavingPreviousSchool(entity.getReasonForLeavingPreviousSchool());
        dto.setEnrollmentActive(enrollmentActive);

        return dto;
    }

    @Override
    public Student toEntity(StudentCreateRequest dto) {
        if (dto == null)
            return null;
        Student entity = new Student();
        entity.setAdmissionNumber(dto.getAdmissionNumber());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setDob(dto.getDob());
        entity.setGender(dto.getGender());
        entity.setPen(dto.getPen());
        entity.setAadharNumber(dto.getAadharNumber());
        entity.setReligion(dto.getReligion());
        entity.setCaste(dto.getCaste());
        entity.setCategory(dto.getCategory());
        entity.setAddress(dto.getAddress());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setPincode(dto.getPincode());
        entity.setContactNumber(dto.getContactNumber());
        entity.setEmail(dto.getEmail());
        entity.setBloodGroup(dto.getBloodGroup());
        entity.setPhotoUrl(dto.getPhotoUrl());
        entity.setDateOfAdmission(dto.getDateOfAdmission());
        entity.setRemarks(dto.getRemarks());
        entity.setSchoolId(TenantContext.getSchoolId());
        entity.setPreviousSchoolName(dto.getPreviousSchoolName());
        entity.setPreviousSchoolBoard(dto.getPreviousSchoolBoard());
        entity.setPreviousClass((dto.getPreviousClass()));
        entity.setPreviousYearOfPassing(dto.getPreviousYearOfPassing());
        entity.setTransferCertificateNumber(dto.getTransferCertificateNumber());
        entity.setPreviousSchoolAddress(dto.getPreviousSchoolAddress());
        entity.setPreviousSchoolContact(dto.getPreviousSchoolContact());
        entity.setReasonForLeavingPreviousSchool(dto.getReasonForLeavingPreviousSchool());

        return entity;
    }
}
