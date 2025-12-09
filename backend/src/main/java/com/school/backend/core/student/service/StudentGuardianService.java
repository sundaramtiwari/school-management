package com.school.backend.core.student.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.guardian.service.GuardianService;
import com.school.backend.core.student.dto.StudentGuardianDto;
import com.school.backend.core.student.entity.StudentGuardian;
import com.school.backend.core.student.repository.StudentGuardianRepository;
import com.school.backend.core.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentGuardianService {

    private final StudentGuardianRepository sgRepo;
    private final StudentRepository studentRepo;
    private final GuardianService guardianService;

    @Transactional
    public StudentGuardianDto linkGuardian(Long studentId, Long guardianId, boolean primary) {
        // validate student
        if (!studentRepo.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }
        // validate guardian using existing guardian repository
        if (!guardianService.existsById(guardianId)) {
            throw new ResourceNotFoundException("Guardian not found: " + guardianId);
        }

        // if primary=true, unset other primary flags for this student
        if (primary) {
            List<StudentGuardian> current = sgRepo.findByStudentId(studentId);
            current.forEach(sg -> {
                if (sg.isPrimaryGuardian()) {
                    sg.setPrimaryGuardian(false);
                    sgRepo.save(sg);
                }
            });
        }

        // if mapping already exists, update primary flag
        boolean exists = sgRepo.existsByStudentIdAndGuardianId(studentId, guardianId);
        if (exists) {
            StudentGuardian sg = sgRepo.findByStudentId(studentId).stream()
                    .filter(s -> s.getGuardianId().equals(guardianId))
                    .findFirst()
                    .orElseThrow(); // should not happen
            sg.setPrimaryGuardian(primary);
            StudentGuardian updated = sgRepo.save(sg);
            return toDto(updated);
        }

        StudentGuardian sg = StudentGuardian.builder()
                .studentId(studentId)
                .guardianId(guardianId)
                .primaryGuardian(primary)
                .build();

        StudentGuardian saved = sgRepo.save(sg);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<StudentGuardianDto> listGuardiansForStudent(Long studentId) {
        if (!studentRepo.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }
        return sgRepo.findByStudentId(studentId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void unlinkGuardian(Long studentId, Long guardianId) {
        List<StudentGuardian> current = sgRepo.findByStudentId(studentId).stream()
                .filter(sg -> sg.getGuardianId().equals(guardianId))
                .collect(Collectors.toList());
        if (current.isEmpty()) {
            throw new ResourceNotFoundException("Guardian mapping not found for student");
        }
        current.forEach(sgRepo::delete);
    }

    private StudentGuardianDto toDto(StudentGuardian sg) {
        StudentGuardianDto d = new StudentGuardianDto();
        d.setId(sg.getId());
        d.setStudentId(sg.getStudentId());
        d.setGuardianId(sg.getGuardianId());
        d.setPrimaryGuardian(sg.isPrimaryGuardian());
        return d;
    }
}
