package com.roberto.climbingwiki.model.entities;

import com.roberto.climbingwiki.model.GradingTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grading_systems")
@Getter
@Setter
@NoArgsConstructor
public class GradingSystem extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GradingTypes.Category category;
}
