package com.school.backend.core.attendance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.school.backend.common.entity.TenantEntity;
import com.school.backend.core.attendance.enums.AttendanceStatus;
import com.school.backend.core.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "student_attendance", uniqueConstraints = {
                @UniqueConstraint(name = "uk_attendance", columnNames = { "class_id", "student_id", "attendance_date",
                                "session_id" })
}, indexes = {
                @Index(name = "idx_attendance_school_date", columnList = "school_id, attendance_date"),
                @Index(name = "idx_attendance_student_date", columnList = "student_id, attendance_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StudentAttendance extends TenantEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "class_id", nullable = false)
        private Long classId;

        @Column(name = "session_id", nullable = false)
        private Long sessionId;

        @Column(name = "student_id", nullable = false)
        private Long studentId;

        @Column(name = "attendance_date", nullable = false)
        private LocalDate attendanceDate;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private AttendanceStatus status;

        private String remarks;

        @JsonIgnore
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "student_id", insertable = false, updatable = false)
        private Student student;
}
