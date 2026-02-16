package com.school.backend.transport.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportEnrollmentDto {
    private Long id;
    private Long studentId;
    private Long pickupPointId;
    private String pickupPointName;
    private Long routeId;
    private String routeName;
    private Long sessionId;
    private boolean active;
}
