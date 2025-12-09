package com.school.backend.core.classsubject.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.classsubject.dto.ClassSubjectDto;
import com.school.backend.core.classsubject.entity.ClassSubject;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.entity.Subject;
import com.school.backend.core.classsubject.mapper.ClassSubjectMapper;
import com.school.backend.core.classsubject.repository.ClassSubjectRepository;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.classsubject.repository.SubjectRepository;
import com.school.backend.core.teacher.entity.Teacher;
import com.school.backend.school.entity.School;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassSubjectService {

    private final ClassSubjectRepository repository;
    private final SchoolClassRepository classRepo;
    private final SubjectRepository subjectRepo;
    private final ClassSubjectMapper mapper;

    public ClassSubjectDto create(ClassSubjectDto dto) {
        if (repository.existsBySchoolClassIdAndSubjectId(dto.getClassId(), dto.getSubjectId())) {
            throw new IllegalArgumentException("Subject already assigned to class.");
        }

        SchoolClass sc = classRepo.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + dto.getClassId()));

        Subject s = subjectRepo.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + dto.getSubjectId()));

        ClassSubject entity = mapper.toEntity(dto);
        entity.setSchoolClass(sc);
        entity.setSubject(s);

        if (dto.getTeacherId() != null) {
            Teacher t = new Teacher();
            t.setId(dto.getTeacherId());
            t.setSchoolId(dto.getSchoolId());
            entity.setTeacher(t);
        }

        School school = new School();
        school.setId(dto.getSchoolId());
        entity.setSchool(school);

        return mapper.toDto(repository.save(entity));
    }

    public ClassSubjectDto getById(Long id) {
        ClassSubject cs = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSubject not found: " + id));
        return mapper.toDto(cs);
    }

    public Page<ClassSubjectDto> getByClass(Long classId, Pageable pageable) {
        return repository.findBySchoolClassId(classId, pageable).map(mapper::toDto);
    }

    public Page<ClassSubjectDto> getBySchool(Long schoolId, Pageable pageable) {
        return repository.findBySchoolId(schoolId, pageable).map(mapper::toDto);
    }

    public Page<ClassSubjectDto> getAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Assignment not found: " + id);
        }
        repository.deleteById(id);
    }
}
