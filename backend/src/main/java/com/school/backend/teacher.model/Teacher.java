package com.school.backend.teacher.model;

import com.school.backend.school.model.School;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teachers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String qualification;
    private String subjectSpecialization;
    private String contactNumber;
    private String email;

    @ManyToOne
    @JoinColumn(name = "school_id")
    private School school;
}
