package com.school.backend.school.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;

    public School create(School school) {
        return schoolRepository.save(school);
    }

    public List<School> getAll() {
        return schoolRepository.findAll();
    }

    public School getByCode(String code) {
        return schoolRepository.findBySchoolCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with code: " + code));
    }
}
