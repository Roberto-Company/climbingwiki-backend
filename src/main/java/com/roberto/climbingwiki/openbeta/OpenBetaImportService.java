package com.roberto.climbingwiki.openbeta;

import com.roberto.climbingwiki.model.entities.Crag;
import com.roberto.climbingwiki.model.entities.Grade;
import com.roberto.climbingwiki.model.entities.GradingSystem;
import com.roberto.climbingwiki.model.entities.Pitch;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Sector;
import com.roberto.climbingwiki.model.entities.Zone;
import com.roberto.climbingwiki.repositories.CragRepository;
import com.roberto.climbingwiki.repositories.GradeRepository;
import com.roberto.climbingwiki.repositories.GradingSystemRepository;
import com.roberto.climbingwiki.repositories.PitchRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.repositories.SectorRepository;
import com.roberto.climbingwiki.repositories.ZoneRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps OpenBeta's recursive Area/Climb tree onto ClimbingWiki's fixed
 * zones -> crags -> sectors -> routes -> pitches hierarchy.
 *
 * <p>OpenBeta has no separate "sector" concept: a leaf Area directly contains
 * climbs. Per the agreed mapping, a leaf Area becomes a {@link Crag} plus a
 * single default {@link Sector} to hold its routes.
 */
@Service
public class OpenBetaImportService {

    static final String EXTERNAL_SOURCE = "OPENBETA";
    private static final String DEFAULT_SECTOR_NAME = "Sector único";
    private static final Pattern TRAILING_YEAR = Pattern.compile("(.*?)[,\\s]*\\b(1[7-9]\\d{2}|20\\d{2})\\b\\s*$");

    private static final List<GradeFieldMapping> GRADE_SYSTEM_PRIORITY = List.of(
            new GradeFieldMapping(OpenBetaGradesDto::french, "French"),
            new GradeFieldMapping(OpenBetaGradesDto::yds, "YDS"),
            new GradeFieldMapping(OpenBetaGradesDto::vscale, "V-Scale"),
            new GradeFieldMapping(OpenBetaGradesDto::font, "Font"),
            new GradeFieldMapping(OpenBetaGradesDto::uiaa, "UIAA"),
            new GradeFieldMapping(OpenBetaGradesDto::ewbank, "Ewbank"));

    private static final Logger log = LoggerFactory.getLogger(OpenBetaImportService.class);

    private final ZoneRepository zoneRepository;
    private final CragRepository cragRepository;
    private final SectorRepository sectorRepository;
    private final RouteRepository routeRepository;
    private final PitchRepository pitchRepository;
    private final GradingSystemRepository gradingSystemRepository;
    private final GradeRepository gradeRepository;

    public OpenBetaImportService(
            ZoneRepository zoneRepository,
            CragRepository cragRepository,
            SectorRepository sectorRepository,
            RouteRepository routeRepository,
            PitchRepository pitchRepository,
            GradingSystemRepository gradingSystemRepository,
            GradeRepository gradeRepository) {
        this.zoneRepository = zoneRepository;
        this.cragRepository = cragRepository;
        this.sectorRepository = sectorRepository;
        this.routeRepository = routeRepository;
        this.pitchRepository = pitchRepository;
        this.gradingSystemRepository = gradingSystemRepository;
        this.gradeRepository = gradeRepository;
    }

    @Transactional
    public OpenBetaImportResult importAreas(List<OpenBetaAreaDto> areas) {
        OpenBetaImportResult.Counters counters = new OpenBetaImportResult.Counters();
        List<OpenBetaAreaDto> byDepth =
                areas.stream().sorted(Comparator.comparingInt(OpenBetaAreaDto::depth)).toList();

        Map<String, Zone> zonesByUuid = new HashMap<>();

        for (OpenBetaAreaDto area : byDepth) {
            if (area.isLeaf()) {
                Zone parentZone = zonesByUuid.get(area.directParentUuid());
                Crag crag = upsertCrag(area, parentZone, counters);
                Sector sector = upsertDefaultSector(crag, counters);
                importClimbs(area, crag, sector, counters);
            } else {
                Zone parentZone = zonesByUuid.get(area.directParentUuid());
                Zone zone = upsertZone(area, parentZone, counters);
                zonesByUuid.put(area.uuid(), zone);
            }
        }

        return counters.toResult();
    }

    /**
     * Maps the same OpenBeta tree the real import would, but never touches the database — use this to
     * sanitize/review data quality (missing names, unmatched grades, duplicated centroid coordinates, the
     * -1 length sentinel) before running {@link #importAreas}.
     */
    public List<OpenBetaPreviewRow> preview(List<OpenBetaAreaDto> areas) {
        List<OpenBetaPreviewRow> rows = new ArrayList<>();
        List<OpenBetaAreaDto> byDepth =
                areas.stream().sorted(Comparator.comparingInt(OpenBetaAreaDto::depth)).toList();

        for (OpenBetaAreaDto area : byDepth) {
            if (area.isLeaf()) {
                rows.add(previewCrag(area));
                if (area.climbs() != null) {
                    for (OpenBetaClimbDto climb : area.climbs()) {
                        rows.add(previewRoute(climb, area));
                    }
                }
            } else {
                rows.add(previewZone(area));
            }
        }
        return rows;
    }

    private OpenBetaPreviewRow previewZone(OpenBetaAreaDto area) {
        List<String> issues = new ArrayList<>();
        if (area.areaName() == null || area.areaName().isBlank()) {
            issues.add("MISSING_NAME");
        }
        return new OpenBetaPreviewRow(
                "ZONE",
                area.uuid(),
                area.directParentUuid(),
                area.areaName(),
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                String.join(";", issues));
    }

    private OpenBetaPreviewRow previewCrag(OpenBetaAreaDto area) {
        List<String> issues = new ArrayList<>();
        if (area.areaName() == null || area.areaName().isBlank()) {
            issues.add("MISSING_NAME");
        }
        Double lat = area.metadata() == null ? null : area.metadata().lat();
        Double lng = area.metadata() == null ? null : area.metadata().lng();
        if (lat == null || lng == null) {
            issues.add("MISSING_COORDINATES");
        }
        return new OpenBetaPreviewRow(
                "CRAG",
                area.uuid(),
                area.directParentUuid(),
                area.areaName(),
                lat,
                lng,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                String.join(";", issues));
    }

    private OpenBetaPreviewRow previewRoute(OpenBetaClimbDto climb, OpenBetaAreaDto parentArea) {
        List<String> issues = new ArrayList<>();
        if (climb.name() == null || climb.name().isBlank()) {
            issues.add("MISSING_NAME");
        }

        Double lat = climb.metadata() == null ? null : climb.metadata().lat();
        Double lng = climb.metadata() == null ? null : climb.metadata().lng();
        Double cragLat = parentArea.metadata() == null ? null : parentArea.metadata().lat();
        Double cragLng = parentArea.metadata() == null ? null : parentArea.metadata().lng();
        if (lat != null && lat.equals(cragLat) && lng != null && lng.equals(cragLng)) {
            issues.add("DUPLICATE_COORDS_WITH_PARENT");
        }

        GradePreviewInfo gradeInfo = previewGrade(climb.grades());
        if (climb.grades() == null || gradeInfo.label() == null) {
            issues.add("NO_GRADE_DATA");
        } else if (!gradeInfo.matched()) {
            issues.add("UNMATCHED_GRADE");
        }

        if (climb.length() != null && climb.length() <= 0) {
            issues.add("LENGTH_UNKNOWN");
        }

        FirstAscent firstAscent = parseFirstAscent(climb.fa());

        return new OpenBetaPreviewRow(
                "ROUTE",
                climb.uuid(),
                parentArea.uuid(),
                climb.name(),
                lat,
                lng,
                resolveRouteType(climb.type()).name(),
                gradeInfo.system(),
                gradeInfo.label(),
                gradeInfo.matched(),
                normalizeLength(climb.length()),
                firstAscent.author(),
                firstAscent.date(),
                String.join(";", issues));
    }

    /** Like {@link #resolveGrade}, but reports the highest-priority non-blank label even when nothing matched. */
    private GradePreviewInfo previewGrade(OpenBetaGradesDto grades) {
        if (grades == null) {
            return new GradePreviewInfo(null, null, false);
        }
        GradePreviewInfo firstNonBlank = null;
        for (GradeFieldMapping mapping : GRADE_SYSTEM_PRIORITY) {
            String label = mapping.extractor().apply(grades);
            if (label == null || label.isBlank()) {
                continue;
            }
            boolean matched = gradingSystemRepository
                    .findByName(mapping.gradingSystemName())
                    .flatMap(system -> gradeRepository.findByGradingSystemIdAndLabel(system.getId(), label))
                    .isPresent();
            if (matched) {
                return new GradePreviewInfo(mapping.gradingSystemName(), label, true);
            }
            if (firstNonBlank == null) {
                firstNonBlank = new GradePreviewInfo(mapping.gradingSystemName(), label, false);
            }
        }
        return firstNonBlank == null ? new GradePreviewInfo(null, null, false) : firstNonBlank;
    }

    private record GradePreviewInfo(String system, String label, boolean matched) {}

    private Zone upsertZone(OpenBetaAreaDto area, Zone parentZone, OpenBetaImportResult.Counters counters) {
        boolean isNew = false;
        Zone zone = zoneRepository.findByExternalSourceAndExternalId(EXTERNAL_SOURCE, area.uuid()).orElse(null);
        if (zone == null) {
            zone = new Zone();
            zone.setVerified(true); // trusted bulk import
            zone.setExternalSource(EXTERNAL_SOURCE);
            zone.setExternalId(area.uuid());
            isNew = true;
        }
        zone.setName(area.areaName());
        zone.setParentZone(parentZone);
        Zone saved = zoneRepository.save(zone);
        if (isNew) {
            counters.zonesCreated++;
        } else {
            counters.zonesUpdated++;
        }
        return saved;
    }

    private Crag upsertCrag(OpenBetaAreaDto area, Zone zone, OpenBetaImportResult.Counters counters) {
        boolean isNew = false;
        Crag crag = cragRepository.findByExternalSourceAndExternalId(EXTERNAL_SOURCE, area.uuid()).orElse(null);
        if (crag == null) {
            crag = new Crag();
            crag.setVerified(true); // trusted bulk import
            crag.setExternalSource(EXTERNAL_SOURCE);
            crag.setExternalId(area.uuid());
            isNew = true;
        }
        crag.setName(area.areaName());
        crag.setZone(zone);
        if (area.metadata() != null) {
            crag.setLatitude(toBigDecimal(area.metadata().lat()));
            crag.setLongitude(toBigDecimal(area.metadata().lng()));
        }
        Crag saved = cragRepository.save(crag);
        if (isNew) {
            counters.cragsCreated++;
        } else {
            counters.cragsUpdated++;
        }
        return saved;
    }

    private Sector upsertDefaultSector(Crag crag, OpenBetaImportResult.Counters counters) {
        String sectorExternalId = crag.getExternalId() + ":default";
        boolean isNew = false;
        Sector sector = sectorRepository
                .findByExternalSourceAndExternalId(EXTERNAL_SOURCE, sectorExternalId)
                .orElse(null);
        if (sector == null) {
            sector = new Sector();
            sector.setVerified(true); // trusted bulk import
            sector.setExternalSource(EXTERNAL_SOURCE);
            sector.setExternalId(sectorExternalId);
            isNew = true;
        }
        sector.setName(DEFAULT_SECTOR_NAME);
        sector.setCrag(crag);
        Sector saved = sectorRepository.save(sector);
        if (isNew) {
            counters.sectorsCreated++;
        } else {
            counters.sectorsUpdated++;
        }
        return saved;
    }

    private void importClimbs(
            OpenBetaAreaDto area, Crag crag, Sector sector, OpenBetaImportResult.Counters counters) {
        if (area.climbs() == null) {
            return;
        }
        for (OpenBetaClimbDto climb : area.climbs()) {
            Route route = upsertRoute(climb, crag, sector, counters);
            upsertPitch(climb, route, counters);
        }
    }

    private Route upsertRoute(
            OpenBetaClimbDto climb, Crag crag, Sector sector, OpenBetaImportResult.Counters counters) {
        boolean isNew = false;
        Route route =
                routeRepository.findByExternalSourceAndExternalId(EXTERNAL_SOURCE, climb.uuid()).orElse(null);
        if (route == null) {
            route = new Route();
            route.setVerified(true); // trusted bulk import
            route.setExternalSource(EXTERNAL_SOURCE);
            route.setExternalId(climb.uuid());
            isNew = true;
        }
        route.setName(climb.name());
        route.setSector(sector);
        route.setRouteType(resolveRouteType(climb.type()));
        if (climb.metadata() != null) {
            BigDecimal lat = toBigDecimal(climb.metadata().lat());
            BigDecimal lng = toBigDecimal(climb.metadata().lng());
            boolean duplicatesCragCoords = sameCoordinates(lat, lng, crag.getLatitude(), crag.getLongitude());
            route.setLatitude(duplicatesCragCoords ? null : lat);
            route.setLongitude(duplicatesCragCoords ? null : lng);
        }
        applyFirstAscent(climb.fa(), route);
        Route saved = routeRepository.save(route);
        if (isNew) {
            counters.routesCreated++;
        } else {
            counters.routesUpdated++;
        }
        return saved;
    }

    private void upsertPitch(OpenBetaClimbDto climb, Route route, OpenBetaImportResult.Counters counters) {
        Pitch pitch =
                pitchRepository.findByRouteIdAndOrderIndex(route.getId(), 1).orElseGet(Pitch::new);
        pitch.setRoute(route);
        pitch.setOrderIndex(1);
        pitch.setLengthM(normalizeLength(climb.length()));
        resolveGrade(climb.grades()).ifPresentOrElse(pitch::setGrade, () -> {
            pitch.setGrade(null);
            if (climb.grades() != null) {
                counters.gradesUnmatched++;
            }
        });
        pitchRepository.save(pitch);
    }

    /** SPORT and TRAD take priority over BOULDER; aid/ice/mixed-only climbs default to TRAD (gear-based). */
    private Route.RouteType resolveRouteType(OpenBetaClimbTypeDto type) {
        if (type == null) {
            return Route.RouteType.TRAD;
        }
        if (Boolean.TRUE.equals(type.sport())) {
            return Route.RouteType.SPORT;
        }
        if (Boolean.TRUE.equals(type.trad())) {
            return Route.RouteType.TRAD;
        }
        if (Boolean.TRUE.equals(type.bouldering())) {
            return Route.RouteType.BOULDER;
        }
        return Route.RouteType.TRAD;
    }

    private Optional<Grade> resolveGrade(OpenBetaGradesDto grades) {
        if (grades == null) {
            return Optional.empty();
        }
        for (GradeFieldMapping mapping : GRADE_SYSTEM_PRIORITY) {
            String label = mapping.extractor().apply(grades);
            if (label == null || label.isBlank()) {
                continue;
            }
            Optional<Grade> grade = gradingSystemRepository
                    .findByName(mapping.gradingSystemName())
                    .flatMap(system -> gradeRepository.findByGradingSystemIdAndLabel(system.getId(), label));
            if (grade.isPresent()) {
                return grade;
            }
            log.debug(
                    "No matching Grade for system '{}' label '{}' — seed it beforehand or it stays unmatched",
                    mapping.gradingSystemName(),
                    label);
        }
        return Optional.empty();
    }

    private void applyFirstAscent(String fa, Route route) {
        FirstAscent parsed = parseFirstAscent(fa);
        route.setFirstAscentBy(parsed.author());
        route.setFirstAscentDate(parsed.date());
    }

    /** Parses free-text "Author, 1998"-style attribution; falls back to storing it verbatim if no year is found. */
    private FirstAscent parseFirstAscent(String fa) {
        if (fa == null || fa.isBlank()) {
            return new FirstAscent(null, null);
        }
        Matcher matcher = TRAILING_YEAR.matcher(fa.trim());
        if (matcher.matches()) {
            String author = matcher.group(1).trim();
            int year = Integer.parseInt(matcher.group(2));
            return new FirstAscent(author.isBlank() ? null : author, LocalDate.of(year, 1, 1));
        }
        return new FirstAscent(fa.trim(), null);
    }

    /** OpenBeta uses -1 (or any non-positive value) as a sentinel for "length unknown". */
    private Integer normalizeLength(Integer length) {
        return length == null || length <= 0 ? null : length;
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private boolean sameCoordinates(BigDecimal lat, BigDecimal lng, BigDecimal otherLat, BigDecimal otherLng) {
        return lat != null
                && lng != null
                && otherLat != null
                && otherLng != null
                && lat.compareTo(otherLat) == 0
                && lng.compareTo(otherLng) == 0;
    }

    private record GradeFieldMapping(Function<OpenBetaGradesDto, String> extractor, String gradingSystemName) {}

    private record FirstAscent(String author, LocalDate date) {}
}
