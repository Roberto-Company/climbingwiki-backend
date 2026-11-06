package com.roberto.climbingwiki.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
public class Route extends BaseEntity {

    public enum RouteType {
        SPORT,
        BOULDER,
        TRAD
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_type", nullable = false, length = 20)
    private RouteType routeType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "equipment_notes", columnDefinition = "TEXT")
    private String equipmentNotes;

    @Column(name = "first_ascent_by", length = 150)
    private String firstAscentBy;

    @Column(name = "first_ascent_date")
    private LocalDate firstAscentDate;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "external_source", length = 50)
    private String externalSource;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "route_tags",
            joinColumns = @JoinColumn(name = "route_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();
}
