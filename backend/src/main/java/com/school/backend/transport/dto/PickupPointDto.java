package com.school.backend.transport.dto;

import com.school.backend.common.enums.FeeFrequency;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PickupPointDto {
    private Long id;
    private String name;
    private BigDecimal amount;
    private FeeFrequency frequency;
    private Long routeId;
}
