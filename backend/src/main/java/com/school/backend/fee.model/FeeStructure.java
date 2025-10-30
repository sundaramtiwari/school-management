package com.school.backend.fee.model;

import com.school.backend.school.model.ClassRoom;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fee_structures")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String term; // e.g., "Q1", "Annual", etc.
    private Double tuitionFee;
    private Double transportFee;
    private Double labFee;
    private Double miscFee;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private ClassRoom classRoom;
}
