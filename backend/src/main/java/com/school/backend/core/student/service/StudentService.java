package com.school.backend.core.student.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.mapper.StudentMapper;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.school.entity.School;
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

    @Transactional
    public StudentDto register(StudentCreateRequest req) {
        // duplicate check
        if (repository.existsByAdmissionNumberAndSchoolId(req.getAdmissionNumber(), req.getSchoolId())) {
            throw new IllegalArgumentException("Admission number already exists for this school");
        }

        // ensure school exists
        School school = schoolRepository.findById(req.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + req.getSchoolId()));

        Student ent = mapper.toEntity(req);
        ent.setSchool(school);
        Student saved = repository.save(ent);
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public StudentDto getById(Long id) {
        Student s = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
        return mapper.toDto(s);
    }

    @Transactional(readOnly = true)
    public Page<StudentDto> listBySchool(Long schoolId, Pageable pageable) {
        return repository.findBySchoolId(schoolId, pageable).map(mapper::toDto);
    }

    @Transactional
    public StudentDto update(Long id, StudentCreateRequest req) {
        Student existing = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
        // update fields (simple approach)
        existing.setFirstName(req.getFirstName());
        existing.setLastName(req.getLastName());
        existing.setDob(req.getDob());
        existing.setGender(req.getGender());
        existing.setContactNumber(req.getContactNumber());
        existing.setCity(req.getCity());
        existing.setRemarks(req.getRemarks());
        // other fields as needed
        return mapper.toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new ResourceNotFoundException("Student not found: " + id);
        // soft delete: mark active=false
        Student s = repository.findById(id).get();
        s.setActive(false);
        repository.save(s);
    }
}
