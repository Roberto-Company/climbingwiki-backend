package com.roberto.climbingwiki.project1.services;

import com.roberto.climbingwiki.project1.dtos.RouteRequest;
import com.roberto.climbingwiki.project1.dtos.RouteResponse;
import com.roberto.climbingwiki.project1.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.SectorNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.TagNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.model.entities.Sector;
import com.roberto.climbingwiki.project1.model.entities.Tag;
import com.roberto.climbingwiki.project1.model.entities.User;
import com.roberto.climbingwiki.project1.repositories.RouteRepository;
import com.roberto.climbingwiki.project1.repositories.SectorRepository;
import com.roberto.climbingwiki.project1.repositories.TagRepository;
import com.roberto.climbingwiki.project1.repositories.UserRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final SectorRepository sectorRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    public RouteService(
            RouteRepository routeRepository,
            SectorRepository sectorRepository,
            UserRepository userRepository,
            TagRepository tagRepository) {
        this.routeRepository = routeRepository;
        this.sectorRepository = sectorRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
    }

    public Page<RouteResponse> findAll(Pageable pageable) {
        return routeRepository.findAll(pageable).map(RouteResponse::from);
    }

    public Page<RouteResponse> findBySectorId(Long sectorId, Pageable pageable) {
        return routeRepository.findBySectorId(sectorId, pageable).map(RouteResponse::from);
    }

    public RouteResponse findById(Long id) {
        return routeRepository.findById(id)
                .map(RouteResponse::from)
                .orElseThrow(() -> new RouteNotFoundException(id));
    }

    @Transactional
    public RouteResponse create(RouteRequest request) {
        Route route = new Route();
        applyRequest(route, request);
        return RouteResponse.from(routeRepository.save(route));
    }

    @Transactional
    public RouteResponse update(Long id, RouteRequest request) {
        Route route = routeRepository.findById(id).orElseThrow(() -> new RouteNotFoundException(id));
        applyRequest(route, request);
        return RouteResponse.from(routeRepository.save(route));
    }

    @Transactional
    public void delete(Long id) {
        if (!routeRepository.existsById(id)) {
            throw new RouteNotFoundException(id);
        }
        routeRepository.deleteById(id);
    }

    private void applyRequest(Route route, RouteRequest request) {
        Sector sector = sectorRepository.findById(request.sectorId())
                .orElseThrow(() -> new SectorNotFoundException(request.sectorId()));
        route.setSector(sector);

        if (request.createdById() != null) {
            User createdBy = userRepository.findById(request.createdById())
                    .orElseThrow(() -> new UserNotFoundException(request.createdById()));
            route.setCreatedBy(createdBy);
        } else {
            route.setCreatedBy(null);
        }

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
