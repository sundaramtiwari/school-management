package com.school.backend.transport.dto;

import com.school.backend.fee.enums.FeeFrequency;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PickupPointDto {
    private Long id;
    private String name;
    private Integer amount;
    private FeeFrequency frequency;
    private Long routeId;
}
