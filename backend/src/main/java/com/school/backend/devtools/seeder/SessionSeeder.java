package com.school.backend.devtools.seeder;

import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SessionSeeder {

    private final AcademicSessionRepository academicSessionRepository;
    private final SchoolRepository schoolRepository;

    @Transactional
    public Result seed(SchoolSeeder.Result schoolResult) {
        List<AcademicSession> sessionsToSave = new ArrayList<>();

        for (School school : schoolResult.schools()) {
            AcademicSession completed = AcademicSession.builder()
                    .schoolId(school.getId())
                    .name("2023-24 (COMPLETED)")
                    .startDate(LocalDate.of(2024, 4, 1))
                    .endDate(LocalDate.of(2025, 3, 31))
                    .active(false)
                    .build();

            AcademicSession active = AcademicSession.builder()
                    .schoolId(school.getId())
                    .name("2024-25 (ACTIVE)")
                    .startDate(LocalDate.of(2025, 4, 1))
                    .endDate(LocalDate.of(2026, 3, 31))
                    .active(true)
                    .build();

            AcademicSession planned = AcademicSession.builder()
                    .schoolId(school.getId())
                    .name("2025-26 (PLANNED)")
                    .startDate(LocalDate.of(2026, 4, 1))
                    .endDate(LocalDate.of(2027, 3, 31))
                    .active(false)
                    .build();

            sessionsToSave.add(completed);
            sessionsToSave.add(active);
            sessionsToSave.add(planned);
        }

        List<AcademicSession> savedSessions = academicSessionRepository.saveAll(sessionsToSave);
        Map<Long, SessionTriplet> sessionsBySchool = new LinkedHashMap<>();

        for (int i = 0; i < savedSessions.size(); i += 3) {
            AcademicSession completed = savedSessions.get(i);
            AcademicSession active = savedSessions.get(i + 1);
            AcademicSession planned = savedSessions.get(i + 2);

            sessionsBySchool.put(completed.getSchoolId(), new SessionTriplet(completed, active, planned));
        }

        List<School> schoolsToUpdate = new ArrayList<>(schoolResult.schools().size());
        for (School school : schoolResult.schools()) {
            SessionTriplet triplet = sessionsBySchool.get(school.getId());
            school.setCurrentSessionId(triplet.active().getId());
            schoolsToUpdate.add(school);
        }
        schoolRepository.saveAll(schoolsToUpdate);

        return new Result(sessionsBySchool);
    }

    public record SessionTriplet(
            AcademicSession completed,
            AcademicSession active,
            AcademicSession planned
    ) {
    }

    public record Result(Map<Long, SessionTriplet> sessionsBySchool) {
    }
}
