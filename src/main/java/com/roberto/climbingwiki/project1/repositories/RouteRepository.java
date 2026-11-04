package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.Route;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RouteRepository extends JpaRepository<Route, Long> {

    Page<Route> findBySectorId(Long sectorId, Pageable pageable);

    Page<Route> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query(
            "select distinct r.sector.id as sectorId, r.routeType as routeType "
                    + "from Route r where r.sector.id in :sectorIds")
    List<SectorRouteType> findDistinctRouteTypesBySectorIdIn(Collection<Long> sectorIds);

    @Query("select distinct r.routeType from Route r where r.sector.crag.zone.id in :zoneIds")
    List<Route.RouteType> findDistinctRouteTypesByZoneIdIn(Collection<Long> zoneIds);

    Optional<Route> findByExternalSourceAndExternalId(String externalSource, String externalId);

    interface SectorRouteType {
        Long getSectorId();

        Route.RouteType getRouteType();
    }
}
