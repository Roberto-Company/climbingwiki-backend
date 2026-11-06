package com.roberto.climbingwiki.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grades")
@Getter
@Setter
@NoArgsConstructor
public class Grade extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_system_id", nullable = false)
    private GradingSystem gradingSystem;

    @Column(nullable = false, length = 20)
    private String label;

    @Column(name = "difficulty_score", nullable = false)
    private Integer difficultyScore;
}
