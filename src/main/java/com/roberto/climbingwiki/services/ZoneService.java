package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.repositories.CatalogSpecifications;
import com.roberto.climbingwiki.dtos.ZoneRequest;
import com.roberto.climbingwiki.dtos.ZoneResponse;
import com.roberto.climbingwiki.exceptions.InvalidZoneParentException;
import com.roberto.climbingwiki.exceptions.ZoneNotFoundException;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Zone;
import com.roberto.climbingwiki.repositories.CragRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.repositories.ZoneRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
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
    private final CragRepository cragRepository;
    private final CatalogAccess access;

    public ZoneService(
            ZoneRepository zoneRepository,
            RouteRepository routeRepository,
            CragRepository cragRepository,
            CatalogAccess access) {
        this.zoneRepository = zoneRepository;
        this.routeRepository = routeRepository;
        this.cragRepository = cragRepository;
        this.access = access;
    }

    /** Lists zones matching every given filter; null or blank filters are ignored. */
    public Page<ZoneResponse> search(Long parentZoneId, boolean rootOnly, String name, Pageable pageable) {
        return zoneRepository
                .findAll(CatalogSpecifications.zoneFilter(parentZoneId, rootOnly, name), pageable)
                .map(zone -> ZoneResponse.from(zone, routeTypesForZone(zone.getId())));
    }

    public ZoneResponse findById(Long id) {
        Zone zone = zoneRepository.findById(id).orElseThrow(() -> new ZoneNotFoundException(id));
        return ZoneResponse.from(zone, routeTypesForZone(id));
    }

    @Transactional
    public ZoneResponse create(ZoneRequest request, AuthenticatedUser requester) {
        Zone zone = new Zone();
        access.stampCreation(zone, requester);
        applyRequest(zone, request, null);
        Zone saved = zoneRepository.save(zone);
        return ZoneResponse.from(saved, Set.of());
    }

    @Transactional
    public ZoneResponse update(Long id, ZoneRequest request, AuthenticatedUser requester) {
        Zone zone = getZone(id);
        access.checkCanEdit(zone, requester);
        applyRequest(zone, request, id);
        Zone saved = zoneRepository.save(zone);
        return ZoneResponse.from(saved, routeTypesForZone(id));
    }

    @Transactional
    public void delete(Long id, AuthenticatedUser requester) {
        Zone zone = getZone(id);
        access.checkCanDelete(zone, requester, zoneRepository.existsByParentZoneId(id) || cragRepository.existsByZoneId(id));
        zoneRepository.deleteById(id);
    }

    @Transactional
    public ZoneResponse setVerified(Long id, boolean verified, AuthenticatedUser requester) {
        Zone zone = getZone(id);
        access.setVerified(zone, verified, requester);
        Zone saved = zoneRepository.save(zone);
        return ZoneResponse.from(saved, routeTypesForZone(id));
    }

    private Zone getZone(Long id) {
        return zoneRepository.findById(id).orElseThrow(() -> new ZoneNotFoundException(id));
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
