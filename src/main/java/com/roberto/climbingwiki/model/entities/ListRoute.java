package com.roberto.climbingwiki.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "list_routes")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class ListRoute {

    @EmbeddedId
    private ListRouteId id = new ListRouteId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("listId")
    @JoinColumn(name = "list_id")
    private RouteList list;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("routeId")
    @JoinColumn(name = "route_id")
    private Route route;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;
}
