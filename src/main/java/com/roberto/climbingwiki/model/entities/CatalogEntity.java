package com.roberto.climbingwiki.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Climbing data anyone can add: zones, crags, sectors, routes and pitches.
 * Data added by regular users stays unverified until a moderator reviews it.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class CatalogEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false)
    private boolean verified;

    public Long getCreatedById() {
        return createdBy != null ? createdBy.getId() : null;
    }
}
