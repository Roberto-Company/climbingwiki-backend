package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.dtos.SectorRequest;
import com.roberto.climbingwiki.dtos.SectorResponse;
import com.roberto.climbingwiki.exceptions.CragNotFoundException;
import com.roberto.climbingwiki.exceptions.SectorNotFoundException;
import com.roberto.climbingwiki.model.entities.Crag;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Sector;
import com.roberto.climbingwiki.repositories.CragRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import com.roberto.climbingwiki.repositories.SectorRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SectorService {

    private final SectorRepository sectorRepository;
    private final CragRepository cragRepository;
    private final RouteRepository routeRepository;
    private final CatalogAccess access;

    public SectorService(
            SectorRepository sectorRepository,
            CragRepository cragRepository,
            RouteRepository routeRepository,
            CatalogAccess access) {
        this.sectorRepository = sectorRepository;
        this.cragRepository = cragRepository;
        this.routeRepository = routeRepository;
        this.access = access;
    }

    public Page<SectorResponse> findAll(Pageable pageable) {
        return toResponsePage(sectorRepository.findAll(pageable));
    }

    public Page<SectorResponse> findByCragId(Long cragId, Pageable pageable) {
        return toResponsePage(sectorRepository.findByCragId(cragId, pageable));
    }

    private Page<SectorResponse> toResponsePage(Page<Sector> sectors) {
        Map<Long, Set<Route.RouteType>> routeTypesBySector =
                routeTypesBySectorIds(sectors.getContent().stream().map(Sector::getId).toList());
        return sectors.map(
                sector -> SectorResponse.from(sector, routeTypesBySector.getOrDefault(sector.getId(), Set.of())));
    }

    public SectorResponse findById(Long id) {
        Sector sector = sectorRepository.findById(id).orElseThrow(() -> new SectorNotFoundException(id));
        Set<Route.RouteType> routeTypes = routeTypesBySectorIds(List.of(id)).getOrDefault(id, Set.of());
        return SectorResponse.from(sector, routeTypes);
    }

    @Transactional
    public SectorResponse create(SectorRequest request, AuthenticatedUser requester) {
        Sector sector = new Sector();
        access.stampCreation(sector, requester);
        applyRequest(sector, request);
        return SectorResponse.from(sectorRepository.save(sector), Set.of());
    }

    @Transactional
    public SectorResponse update(Long id, SectorRequest request, AuthenticatedUser requester) {
        Sector sector = getSector(id);
        access.checkCanEdit(sector, requester);
        applyRequest(sector, request);
        Sector saved = sectorRepository.save(sector);
        Set<Route.RouteType> routeTypes = routeTypesBySectorIds(List.of(id)).getOrDefault(id, Set.of());
        return SectorResponse.from(saved, routeTypes);
    }

    @Transactional
    public void delete(Long id, AuthenticatedUser requester) {
        Sector sector = getSector(id);
        access.checkCanDelete(sector, requester, routeRepository.existsBySectorId(id));
        sectorRepository.deleteById(id);
    }

    @Transactional
    public SectorResponse setVerified(Long id, boolean verified, AuthenticatedUser requester) {
        Sector sector = getSector(id);
        access.setVerified(sector, verified, requester);
        Sector saved = sectorRepository.save(sector);
        return SectorResponse.from(saved, routeTypesBySectorIds(List.of(id)).getOrDefault(id, Set.of()));
    }

    private Sector getSector(Long id) {
        return sectorRepository.findById(id).orElseThrow(() -> new SectorNotFoundException(id));
    }

    private void applyRequest(Sector sector, SectorRequest request) {
        Crag crag = cragRepository.findById(request.cragId())
                .orElseThrow(() -> new CragNotFoundException(request.cragId()));
        sector.setCrag(crag);
        sector.setName(request.name());
        sector.setDescription(request.description());
        sector.setLatitude(request.latitude());
        sector.setLongitude(request.longitude());
    }

    private Map<Long, Set<Route.RouteType>> routeTypesBySectorIds(List<Long> sectorIds) {
        if (sectorIds.isEmpty()) {
            return Map.of();
        }
        return routeRepository.findDistinctRouteTypesBySectorIdIn(sectorIds).stream()
                .collect(Collectors.groupingBy(
                        RouteRepository.SectorRouteType::getSectorId,
                        Collectors.mapping(RouteRepository.SectorRouteType::getRouteType, Collectors.toSet())));
    }
}
