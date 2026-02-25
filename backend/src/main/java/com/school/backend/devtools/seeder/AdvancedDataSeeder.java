package com.school.backend.devtools.seeder;

import com.school.backend.common.enums.UserRole;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class AdvancedDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SchoolRepository schoolRepository;
    private final SchoolSeeder schoolSeeder;
    private final SessionSeeder sessionSeeder;
    private final ClassSubjectSeeder classSubjectSeeder;
    private final StudentSeeder studentSeeder;
    private final GuardianSeeder guardianSeeder;
    private final TransportSeeder transportSeeder;
    private final FeeSeeder feeSeeder;
    private final ExamSeeder examSeeder;
    private final FinanceVerificationSeeder financeVerificationSeeder;

    @Override
    public void run(String... args) {
        createSuperAdminIfMissing();

        if (schoolRepository.count() > 0) {
            log.info("Advanced data seeding skipped because schools already exist.");
            financeVerificationSeeder.seed();
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
        financeVerificationSeeder.seed();

        log.info("Advanced data seeding completed.");
    }

    private void createSuperAdminIfMissing() {
        if (userRepository.existsByEmail("admin@school.com")) {
            return;
        }

        User admin = User.builder()
                .email("admin@school.com")
                .passwordHash(passwordEncoder.encode("admin"))
                .role(UserRole.SUPER_ADMIN)
                .active(true)
                .build();

        userRepository.save(admin);
        System.out.println("CREATED SUPER ADMIN: admin@school.com / admin");
    }
}
