package com.alumni.academic_management_api.entity;

import com.alumni.academic_management_api.enums.Level;
import com.alumni.academic_management_api.enums.Modality;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "courses")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false)
    @ToString.Include
    private String name;

    @Column(name = "level", nullable = false)
    @Enumerated(EnumType.STRING)
    @ToString.Include
    private Level level;

    @Column(name = "modality", nullable = false)
    @Enumerated(EnumType.STRING)
    @ToString.Include
    private Modality modality;
}
