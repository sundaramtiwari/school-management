package com.school.backend.core.student.entity;

import com.school.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_documents", indexes = {
        @Index(name = "idx_student_document_student", columnList = "student_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class StudentDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    private String fileType;   // e.g. "Birth Certificate"
    private String fileName;   // original file name
    private String fileUrl;    // stored location link (S3/local/DB)
    private LocalDateTime uploadedAt;
    private String remarks;
}
