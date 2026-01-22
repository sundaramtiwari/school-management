package com.school.backend.testmanagement.service;

import com.school.backend.testmanagement.dto.GradePolicyRequest;
import com.school.backend.testmanagement.entity.GradePolicy;
import com.school.backend.testmanagement.repository.GradePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GradePolicyService {

    private final GradePolicyRepository repo;

    @Transactional
    public GradePolicy create(GradePolicyRequest req) {

        GradePolicy gp = GradePolicy.builder()
                .schoolId(req.getSchoolId())
                .minPercent(req.getMinPercent())
                .maxPercent(req.getMaxPercent())
                .grade(req.getGrade())
                .build();

        return repo.save(gp);
    }

    @Transactional(readOnly = true)
    public List<GradePolicy> getForSchool(Long schoolId) {
        return repo.findBySchoolIdOrderByMinPercentDesc(schoolId);
    }
}
