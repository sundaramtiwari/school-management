package com.school.backend.user.repository;

import com.school.backend.common.enums.UserRole;
import com.school.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
                extends JpaRepository<User, Long> {

        Optional<User> findByEmailAndActiveTrue(String email);

        boolean existsByEmail(String email);

        Page<User> findByRole(UserRole role, Pageable pageable);

        long countBySchoolIdAndRole(Long schoolId, UserRole role);
}
