package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.repositories.CatalogSpecifications;
import com.roberto.climbingwiki.dtos.RouteRequest;
import com.roberto.climbingwiki.dtos.RouteResponse;
import com.roberto.climbingwiki.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.exceptions.SectorNotFoundException;
import com.roberto.climbingwiki.exceptions.TagNotFoundException;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Sector;
import com.roberto.climbingwiki.model.entities.Tag;
import com.roberto.climbingwiki.dtos.RouteSummary;
import com.roberto.climbingwiki.repositories.PitchRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.repositories.SectorRepository;
import com.roberto.climbingwiki.repositories.TagRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final SectorRepository sectorRepository;
    private final TagRepository tagRepository;
    private final PitchRepository pitchRepository;
    private final CatalogAccess access;

    public RouteService(
            RouteRepository routeRepository,
            SectorRepository sectorRepository,
            TagRepository tagRepository,
            PitchRepository pitchRepository,
            CatalogAccess access) {
        this.routeRepository = routeRepository;
        this.sectorRepository = sectorRepository;
        this.tagRepository = tagRepository;
        this.pitchRepository = pitchRepository;
        this.access = access;
    }

    /** Lists routes matching every given filter; null or blank filters are ignored. */
    public Page<RouteResponse> search(Long sectorId, String name, Pageable pageable) {
        Page<Route> routes = routeRepository.findAll(CatalogSpecifications.routeFilter(sectorId, name), pageable);
        Map<Long, RouteSummary> summaries = summarize(routes.getContent());
        return routes.map(route -> toResponse(route, summaries));
    }

    public RouteResponse findById(Long id) {
        return routeRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RouteNotFoundException(id));
    }

    @Transactional
    public RouteResponse create(RouteRequest request, AuthenticatedUser requester) {
        Route route = new Route();
        access.stampCreation(route, requester);
        applyRequest(route, request);
        return RouteResponse.from(routeRepository.save(route), RouteSummary.EMPTY);
    }

    @Transactional
    public RouteResponse update(Long id, RouteRequest request, AuthenticatedUser requester) {
        Route route = getRoute(id);
        access.checkCanEdit(route, requester);
        applyRequest(route, request);
        return toResponse(routeRepository.save(route));
    }

    @Transactional
    public void delete(Long id, AuthenticatedUser requester) {
        Route route = getRoute(id);
        access.checkCanDelete(route, requester, pitchRepository.existsByRouteId(id));
        routeRepository.deleteById(id);
    }

    @Transactional
    public RouteResponse setVerified(Long id, boolean verified, AuthenticatedUser requester) {
        Route route = getRoute(id);
        access.setVerified(route, verified, requester);
        Route saved = routeRepository.save(route);
        return toResponse(saved);
    }

    private Route getRoute(Long id) {
        return routeRepository.findById(id).orElseThrow(() -> new RouteNotFoundException(id));
    }

    /** One query for the pitches of all given routes, instead of one per route. */
    private Map<Long, RouteSummary> summarize(List<Route> routes) {
        if (routes.isEmpty()) {
            return Map.of();
        }
        return RouteSummary.byRoute(pitchRepository.findSummaryRows(routes.stream().map(Route::getId).toList()));
    }

    private RouteResponse toResponse(Route route) {
        return toResponse(route, summarize(List.of(route)));
    }

    private static RouteResponse toResponse(Route route, Map<Long, RouteSummary> summaries) {
        return RouteResponse.from(route, summaries.getOrDefault(route.getId(), RouteSummary.EMPTY));
    }

    private void applyRequest(Route route, RouteRequest request) {
        Sector sector = sectorRepository.findById(request.sectorId())
                .orElseThrow(() -> new SectorNotFoundException(request.sectorId()));
        route.setSector(sector);

        route.setName(request.name());
        route.setRouteType(request.routeType());
        route.setDescription(request.description());
        route.setEquipmentNotes(request.equipmentNotes());
        route.setFirstAscentBy(request.firstAscentBy());
        route.setFirstAscentDate(request.firstAscentDate());
        route.setLatitude(request.latitude());
        route.setLongitude(request.longitude());
        route.setTags(resolveTags(request.tagIds()));
    }

    private Set<Tag> resolveTags(Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Tag> tags = new HashSet<>(tagRepository.findAllById(tagIds));
        if (tags.size() != tagIds.size()) {
            throw new TagNotFoundException(tagIds);
        }
        return tags;
    }
}
