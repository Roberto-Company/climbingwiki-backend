package com.roberto.climbingwiki.project1.services;

import com.roberto.climbingwiki.project1.dtos.ZoneRequest;
import com.roberto.climbingwiki.project1.dtos.ZoneResponse;
import com.roberto.climbingwiki.project1.exceptions.InvalidZoneParentException;
import com.roberto.climbingwiki.project1.exceptions.ZoneNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.model.entities.Zone;
import com.roberto.climbingwiki.project1.repositories.RouteRepository;
import com.roberto.climbingwiki.project1.repositories.ZoneRepository;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final RouteRepository routeRepository;

    public ZoneService(ZoneRepository zoneRepository, RouteRepository routeRepository) {
        this.zoneRepository = zoneRepository;
        this.routeRepository = routeRepository;
    }

    public Page<ZoneResponse> findAll(Pageable pageable) {
        return zoneRepository.findAll(pageable).map(zone -> ZoneResponse.from(zone, routeTypesForZone(zone.getId())));
    }

    public Page<ZoneResponse> findByParentZoneId(Long parentZoneId, Pageable pageable) {
        return zoneRepository
                .findByParentZoneId(parentZoneId, pageable)
                .map(zone -> ZoneResponse.from(zone, routeTypesForZone(zone.getId())));
    }

    public Page<ZoneResponse> findRootZones(Pageable pageable) {
        return zoneRepository
                .findByParentZoneIsNull(pageable)
                .map(zone -> ZoneResponse.from(zone, routeTypesForZone(zone.getId())));
    }

    public ZoneResponse findById(Long id) {
        Zone zone = zoneRepository.findById(id).orElseThrow(() -> new ZoneNotFoundException(id));
        return ZoneResponse.from(zone, routeTypesForZone(id));
    }

    @Transactional
    public ZoneResponse create(ZoneRequest request) {
        Zone zone = new Zone();
        applyRequest(zone, request, null);
        Zone saved = zoneRepository.save(zone);
        return ZoneResponse.from(saved, Set.of());
    }

    @Transactional
    public ZoneResponse update(Long id, ZoneRequest request) {
        Zone zone = zoneRepository.findById(id).orElseThrow(() -> new ZoneNotFoundException(id));
        applyRequest(zone, request, id);
        Zone saved = zoneRepository.save(zone);
        return ZoneResponse.from(saved, routeTypesForZone(id));
    }

    @Transactional
    public void delete(Long id) {
        if (!zoneRepository.existsById(id)) {
            throw new ZoneNotFoundException(id);
        }
        zoneRepository.deleteById(id);
    }

    private void applyRequest(Zone zone, ZoneRequest request, Long selfId) {
        if (request.parentZoneId() != null) {
            if (request.parentZoneId().equals(selfId)) {
                throw new InvalidZoneParentException(selfId);
            }
            Zone parentZone = zoneRepository
                    .findById(request.parentZoneId())
                    .orElseThrow(() -> new ZoneNotFoundException(request.parentZoneId()));
            zone.setParentZone(parentZone);
        } else {
            zone.setParentZone(null);
        }

        zone.setName(request.name());
        zone.setDescription(request.description());
    }

    private Set<Route.RouteType> routeTypesForZone(Long zoneId) {
        return new HashSet<>(routeRepository.findDistinctRouteTypesByZoneIdIn(collectZoneAndDescendantIds(zoneId)));
    }

    private Set<Long> collectZoneAndDescendantIds(Long zoneId) {
        Set<Long> ids = new HashSet<>();
        Deque<Long> toVisit = new ArrayDeque<>();
        toVisit.add(zoneId);
        while (!toVisit.isEmpty()) {
            Long current = toVisit.poll();
            if (!ids.add(current)) {
                continue;
            }
            zoneRepository.findByParentZoneId(current).forEach(child -> toVisit.add(child.getId()));
        }
        return ids;
    }
}
