package com.school.backend.core.student.mapper;

import com.school.backend.core.student.dto.StudentEnrollmentDto;
import com.school.backend.core.student.dto.StudentEnrollmentRequest;
import com.school.backend.core.student.entity.StudentEnrollment;
import org.springframework.stereotype.Component;

@Component
public class StudentEnrollmentMapperImpl implements StudentEnrollmentMapper {

    @Override
    public StudentEnrollmentDto toDto(StudentEnrollment e) {
        if (e == null) return null;
        StudentEnrollmentDto d = new StudentEnrollmentDto();
        d.setId(e.getId());
        d.setStudentId(e.getStudentId());
        d.setClassId(e.getClassId());
        d.setSection(e.getSection());
        d.setSession(e.getSession());
        d.setRollNumber(e.getRollNumber());
        d.setEnrollmentDate(e.getEnrollmentDate());
        d.setActive(e.isActive());
        d.setRemarks(e.getRemarks());
        return d;
    }

    @Override
    public StudentEnrollment toEntity(StudentEnrollmentRequest r) {
        if (r == null) return null;
        StudentEnrollment e = new StudentEnrollment();
        e.setStudentId(r.getStudentId());
        e.setClassId(r.getClassId());
        e.setSection(r.getSection());
        e.setSession(r.getSession());
        e.setRollNumber(r.getRollNumber());
        e.setEnrollmentDate(r.getEnrollmentDate());
        e.setActive(true);
        e.setRemarks(r.getRemarks());
        return e;
    }
}
