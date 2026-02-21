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
        LocalDate today = LocalDate.now();
        int activeStartYear = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
        int completedStartYear = activeStartYear - 1;
        int plannedStartYear = activeStartYear + 1;

        for (School school : schoolResult.schools()) {
            AcademicSession completed = AcademicSession.builder()
                    .schoolId(school.getId())
                    .name(sessionName(completedStartYear))
                    .startDate(sessionStart(completedStartYear))
                    .endDate(sessionEnd(completedStartYear))
                    .active(false)
                    .build();

            AcademicSession active = AcademicSession.builder()
                    .schoolId(school.getId())
                    .name(sessionName(activeStartYear))
                    .startDate(sessionStart(activeStartYear))
                    .endDate(sessionEnd(activeStartYear))
                    .active(true)
                    .build();

            AcademicSession planned = AcademicSession.builder()
                    .schoolId(school.getId())
                    .name(sessionName(plannedStartYear))
                    .startDate(sessionStart(plannedStartYear))
                    .endDate(sessionEnd(plannedStartYear))
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

    private String sessionName(int startYear) {
        int endYearShort = (startYear + 1) % 100;
        return startYear + "-" + String.format("%02d", endYearShort);
    }

    private LocalDate sessionStart(int startYear) {
        return LocalDate.of(startYear, 4, 1);
    }

    private LocalDate sessionEnd(int startYear) {
        return LocalDate.of(startYear + 1, 3, 31);
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
