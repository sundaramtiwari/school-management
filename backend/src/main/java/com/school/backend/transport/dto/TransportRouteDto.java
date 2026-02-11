package com.school.backend.transport.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportRouteDto {
    private Long id;
    private String name;
    private String description;
}
