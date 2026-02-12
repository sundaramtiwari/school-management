package com.school.backend.transport.entity;

import com.school.backend.common.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "transport_enrollments", uniqueConstraints = {
                @UniqueConstraint(columnNames = { "student_id", "session_id" })
}, indexes = {
                @Index(name = "idx_transport_enrollment_student", columnList = "student_id,session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TransportEnrollment extends TenantEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "student_id", nullable = false)
        private Long studentId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "pickup_point_id", nullable = false)
        private PickupPoint pickupPoint;

        @Column(name = "session_id", nullable = false)
        private Long sessionId;

        @Builder.Default
        private boolean active = true;
}
