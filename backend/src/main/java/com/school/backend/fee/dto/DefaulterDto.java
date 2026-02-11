package com.school.backend.fee.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DefaulterDto {

    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String className;
    private String classSection;
    private long amountDue;
    private LocalDate lastPaymentDate;
    private long daysOverdue;
    private String parentContact;
}
