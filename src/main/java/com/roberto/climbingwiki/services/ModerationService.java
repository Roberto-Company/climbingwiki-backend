package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.dtos.UnverifiedItem;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.model.entities.BaseEntity;
import com.roberto.climbingwiki.model.entities.CatalogEntity;
import com.roberto.climbingwiki.model.entities.Crag;
import com.roberto.climbingwiki.model.entities.Pitch;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Sector;
import com.roberto.climbingwiki.model.entities.Zone;
import com.roberto.climbingwiki.repositories.CragRepository;
import com.roberto.climbingwiki.repositories.PitchRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.repositories.SectorRepository;
import com.roberto.climbingwiki.repositories.ZoneRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The review queue: climbing data added by regular users that no moderator has verified yet. */
@Service
public class ModerationService {

    private final ZoneRepository zoneRepository;
    private final CragRepository cragRepository;
    private final SectorRepository sectorRepository;
    private final RouteRepository routeRepository;
    private final PitchRepository pitchRepository;

    public ModerationService(
            ZoneRepository zoneRepository,
            CragRepository cragRepository,
            SectorRepository sectorRepository,
            RouteRepository routeRepository,
            PitchRepository pitchRepository) {
        this.zoneRepository = zoneRepository;
        this.cragRepository = cragRepository;
        this.sectorRepository = sectorRepository;
        this.routeRepository = routeRepository;
        this.pitchRepository = pitchRepository;
    }

    /** Up to 100 newest unverified items of each kind, zones first and pitches last. */
    @Transactional(readOnly = true)
    public List<UnverifiedItem> unverifiedItems(AuthenticatedUser requester) {
        if (!requester.isModeratorOrAdmin()) {
            throw new ForbiddenException();
        }
        return Stream.of(
                        zoneRepository.findTop100ByVerifiedFalseOrderByIdDesc().stream()
                                .map(zone -> item("zone", zone, zone.getName(), null, zone.getParentZone())),
                        cragRepository.findTop100ByVerifiedFalseOrderByIdDesc().stream()
                                .map(crag -> item("crag", crag, crag.getName(), null, crag.getZone())),
                        sectorRepository.findTop100ByVerifiedFalseOrderByIdDesc().stream()
                                .map(sector -> item("sector", sector, sector.getName(), null, sector.getCrag())),
                        routeRepository.findTop100ByVerifiedFalseOrderByIdDesc().stream()
                                .map(route -> item("route", route, route.getName(), null, route.getSector())),
                        pitchRepository.findTop100ByVerifiedFalseOrderByIdDesc().stream()
                                .map(this::pitchItem))
                .flatMap(items -> items)
                .toList();
    }

    private UnverifiedItem pitchItem(Pitch pitch) {
        Route route = pitch.getRoute();
        return item("pitch", pitch, route.getName(), pitch.getOrderIndex(), route);
    }

    private static UnverifiedItem item(
            String kind, CatalogEntity entity, String name, Integer pitchNumber, BaseEntity parent) {
        return new UnverifiedItem(
                kind,
                entity.getId(),
                name,
                pitchNumber,
                parent != null ? parent.getId() : null,
                parentName(parent),
                entity.getCreatedById(),
                entity.getCreatedBy() != null ? entity.getCreatedBy().getUsername() : null);
    }

    private static String parentName(BaseEntity parent) {
        return switch (parent) {
            case null -> null;
            case Zone zone -> zone.getName();
            case Crag crag -> crag.getName();
            case Sector sector -> sector.getName();
            case Route route -> route.getName();
            default -> null;
        };
    }
}
