package com.school.backend.exam.model;

import com.school.backend.school.model.ClassRoom;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "exams")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // e.g., "Mid-Term"
    private LocalDate examDate;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private ClassRoom classRoom;
}
