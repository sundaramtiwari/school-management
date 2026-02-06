package com.school.backend.common.config;

import com.school.backend.common.enums.UserRole;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            createSuperAdmin();
        }
    }

    private void createSuperAdmin() {
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
