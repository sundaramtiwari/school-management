package com.school.backend.student.model;

import com.school.backend.school.model.ClassRoom;
import com.school.backend.school.model.School;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String gender;
    private LocalDate dateOfBirth;

    private String admissionNumber;
    private LocalDate admissionDate;

    private String address;
    private String contactNumber;

    @ManyToOne
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private ClassRoom classRoom;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Parent parent;
}
