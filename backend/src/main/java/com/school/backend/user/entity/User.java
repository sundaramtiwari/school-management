package com.school.backend.user.entity;

import com.school.backend.common.entity.BaseEntity;
import com.school.backend.school.entity.School;
import com.school.backend.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_users_email",
            columnNames = "email"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(
    callSuper = false,
    onlyExplicitlyIncluded = true
)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    /**
     * Null for SUPER_ADMIN
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @Builder.Default
    private boolean active = true;
}
