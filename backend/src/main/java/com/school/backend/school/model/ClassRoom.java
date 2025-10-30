package com.school.backend.school.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "class_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String className; // e.g., "10"
    private String section;   // e.g., "A"

    @ManyToOne
    @JoinColumn(name = "school_id")
    private School school;
}
