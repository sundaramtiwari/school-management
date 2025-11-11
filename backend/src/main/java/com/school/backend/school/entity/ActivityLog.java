package com.school.backend.school.entity;

import com.school.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class ActivityLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long schoolId;
    private Long userId;

    private String action;      // e.g. "Created Student"
    private String entityType;  // e.g. STUDENT, FEE, TEST
    private Long entityId;

    private String ipAddress;
    private LocalDateTime timestamp;
}
