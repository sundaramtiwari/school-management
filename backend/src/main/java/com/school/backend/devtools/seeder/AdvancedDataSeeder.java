package com.school.backend.devtools.seeder;

import com.school.backend.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class AdvancedDataSeeder implements CommandLineRunner {

    private final SchoolRepository schoolRepository;
    private final SchoolSeeder schoolSeeder;
    private final SessionSeeder sessionSeeder;
    private final ClassSubjectSeeder classSubjectSeeder;
    private final StudentSeeder studentSeeder;
    private final GuardianSeeder guardianSeeder;
    private final TransportSeeder transportSeeder;
    private final FeeSeeder feeSeeder;
    private final ExamSeeder examSeeder;

    @Override
    public void run(String... args) {
        if (schoolRepository.count() > 0) {
            log.info("Advanced data seeding skipped because schools already exist.");
            return;
        }

        Random random = new Random(42);

        SchoolSeeder.Result schoolResult = schoolSeeder.seed(random);
        SessionSeeder.Result sessionResult = sessionSeeder.seed(schoolResult);
        ClassSubjectSeeder.Result classSubjectResult = classSubjectSeeder.seed(random, schoolResult, sessionResult);
        StudentSeeder.Result studentResult = studentSeeder.seed(random, schoolResult, sessionResult, classSubjectResult);
        guardianSeeder.seed(studentResult);
        TransportSeeder.Result transportResult = transportSeeder.seed(random, sessionResult, studentResult);
        feeSeeder.seed(random, sessionResult, classSubjectResult, studentResult, transportResult);
        examSeeder.seed(random, classSubjectResult, studentResult);

        log.info("Advanced data seeding completed.");
    }
}
