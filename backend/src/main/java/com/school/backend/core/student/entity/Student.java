package com.school.backend.core.student.entity;

import com.school.backend.common.entity.BaseEntity;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.school.entity.School;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_student_school_adm", columnList = "school_id,admission_number"),
        @Index(name = "idx_student_school_aadhar", columnList = "school_id,aadhar_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "admission_number", nullable = false)
    private String admissionNumber;

    @Column(nullable = false)
    private String firstName;

    private String lastName;

    private LocalDate dob;

    @Column(nullable = false)
    private String gender;

    private String pen;
    @Column(name = "aadhar_number")
    private String aadharNumber;

    private String religion;
    private String caste;
    private String category;

    private String address;
    private String city;
    private String state;
    private String pincode;

    private String contactNumber;
    private String email;
    private String bloodGroup;
    private String photoUrl;

    private LocalDate dateOfAdmission;
    private LocalDate dateOfLeaving;
    private String reasonForLeaving;

    @Builder.Default
    private boolean active = true;

    @Column(name = "current_status")
    private String currentStatus; // ENROLLED, PASSED_OUT, LEFT, SUSPENDED

    // currentClass references class id for quick lookup; keep as relation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_class_id")
    private SchoolClass currentClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    private String remarks;
}
