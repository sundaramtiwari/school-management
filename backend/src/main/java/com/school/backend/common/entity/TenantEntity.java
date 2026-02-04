package com.school.backend.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;


@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FilterDef(
        name = "tenantFilter",
        parameters = @ParamDef(
                name = "schoolId",
                type = Long.class
        )
)
@Filter(
        name = "tenantFilter",
        condition = "school_id = :schoolId"
)
public abstract class TenantEntity extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    protected Long schoolId;

}
