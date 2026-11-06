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
@Table(name = "pitches")
@Getter
@Setter
@NoArgsConstructor
public class Pitch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id")
    private Grade grade;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "length_m")
    private Integer lengthM;

    @Column(name = "bolts_count")
    private Integer boltsCount;

    @Column(columnDefinition = "TEXT")
    private String description;
}
