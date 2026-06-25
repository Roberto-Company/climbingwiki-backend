package com.roberto.climbingwiki.project1.openbeta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.project1.model.entities.Crag;
import com.roberto.climbingwiki.project1.model.entities.Grade;
import com.roberto.climbingwiki.project1.model.entities.GradingSystem;
import com.roberto.climbingwiki.project1.model.entities.Pitch;
import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.model.entities.Sector;
import com.roberto.climbingwiki.project1.model.entities.Zone;
import com.roberto.climbingwiki.project1.repositories.CragRepository;
import com.roberto.climbingwiki.project1.repositories.GradeRepository;
import com.roberto.climbingwiki.project1.repositories.GradingSystemRepository;
import com.roberto.climbingwiki.project1.repositories.PitchRepository;
import com.roberto.climbingwiki.project1.repositories.RouteRepository;
import com.roberto.climbingwiki.project1.repositories.SectorRepository;
import com.roberto.climbingwiki.project1.repositories.ZoneRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenBetaImportServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private CragRepository cragRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private PitchRepository pitchRepository;

    @Mock
    private GradingSystemRepository gradingSystemRepository;

    @Mock
    private GradeRepository gradeRepository;

    private OpenBetaImportService importService;
    private final AtomicLong idSequence = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        importService = new OpenBetaImportService(
                zoneRepository,
                cragRepository,
                sectorRepository,
                routeRepository,
                pitchRepository,
                gradingSystemRepository,
                gradeRepository);

        lenient().when(zoneRepository.save(any(Zone.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        lenient().when(cragRepository.save(any(Crag.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        lenient()
                .when(sectorRepository.save(any(Sector.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
        lenient()
                .when(routeRepository.save(any(Route.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
        lenient().when(pitchRepository.save(any(Pitch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(pitchRepository.findByRouteIdAndOrderIndex(anyLong(), eq(1))).thenReturn(Optional.empty());
    }

    private <T> T withId(Object entity) {
        if (entity instanceof Zone zone && zone.getId() == null) {
            zone.setId(idSequence.getAndIncrement());
        } else if (entity instanceof Crag crag && crag.getId() == null) {
            crag.setId(idSequence.getAndIncrement());
        } else if (entity instanceof Sector sector && sector.getId() == null) {
            sector.setId(idSequence.getAndIncrement());
        } else if (entity instanceof Route route && route.getId() == null) {
            route.setId(idSequence.getAndIncrement());
        }
        @SuppressWarnings("unchecked")
        T result = (T) entity;
        return result;
    }

    private OpenBetaAreaDto zoneArea(String uuid, String name, List<String> ancestors) {
        return new OpenBetaAreaDto(
                uuid, name, ancestors == null ? List.of(name) : List.of(name), ancestors, new OpenBetaAreaMetadataDto(false, null, null), null);
    }

    private OpenBetaAreaDto leafArea(
            String uuid, String name, List<String> ancestors, Double lat, Double lng, List<OpenBetaClimbDto> climbs) {
        return new OpenBetaAreaDto(
                uuid, name, List.of(name), ancestors, new OpenBetaAreaMetadataDto(true, lat, lng), climbs);
    }

    private OpenBetaClimbDto climb(
            String uuid, String name, OpenBetaClimbTypeDto type, OpenBetaGradesDto grades, String fa, Integer length) {
        return new OpenBetaClimbDto(uuid, name, fa, length, type, grades, new OpenBetaClimbMetadataDto(1.0, 2.0));
    }

    @Test
    void importAreas_createsZoneCragSectorAndRoute_withResolvedGrade() {
        OpenBetaAreaDto zone = zoneArea("zone-1", "Andalucía", List.of());
        OpenBetaClimbDto climb = climb(
                "climb-1",
                "Vía X",
                new OpenBetaClimbTypeDto(true, false, false, false, false, false),
                new OpenBetaGradesDto("6a", null, null, null, null, null),
                "J. García, 1998",
                25);
        OpenBetaAreaDto crag = leafArea("crag-1", "El Chorro", List.of("zone-1"), 36.9123, -4.7501, List.of(climb));

        GradingSystem french = new GradingSystem();
        french.setId(10L);
        Grade grade6a = new Grade();
        grade6a.setId(99L);
        when(gradingSystemRepository.findByName("French")).thenReturn(Optional.of(french));
        when(gradeRepository.findByGradingSystemIdAndLabel(10L, "6a")).thenReturn(Optional.of(grade6a));

        OpenBetaImportResult result = importService.importAreas(List.of(zone, crag));

        assertThat(result.zonesCreated()).isEqualTo(1);
        assertThat(result.cragsCreated()).isEqualTo(1);
        assertThat(result.sectorsCreated()).isEqualTo(1);
        assertThat(result.routesCreated()).isEqualTo(1);
        assertThat(result.gradesUnmatched()).isZero();

        ArgumentCaptor<Crag> cragCaptor = ArgumentCaptor.forClass(Crag.class);
        org.mockito.Mockito.verify(cragRepository).save(cragCaptor.capture());
        assertThat(cragCaptor.getValue().getZone().getName()).isEqualTo("Andalucía");
        assertThat(cragCaptor.getValue().getLatitude()).isEqualByComparingTo("36.9123");

        ArgumentCaptor<Sector> sectorCaptor = ArgumentCaptor.forClass(Sector.class);
        org.mockito.Mockito.verify(sectorRepository).save(sectorCaptor.capture());
        assertThat(sectorCaptor.getValue().getName()).isEqualTo("Sector único");

        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        org.mockito.Mockito.verify(routeRepository).save(routeCaptor.capture());
        Route savedRoute = routeCaptor.getValue();
        assertThat(savedRoute.getRouteType()).isEqualTo(Route.RouteType.SPORT);
        assertThat(savedRoute.getFirstAscentBy()).isEqualTo("J. García");
        assertThat(savedRoute.getFirstAscentDate()).isEqualTo(LocalDate.of(1998, 1, 1));

        ArgumentCaptor<Pitch> pitchCaptor = ArgumentCaptor.forClass(Pitch.class);
        org.mockito.Mockito.verify(pitchRepository).save(pitchCaptor.capture());
        assertThat(pitchCaptor.getValue().getGrade()).isEqualTo(grade6a);
        assertThat(pitchCaptor.getValue().getLengthM()).isEqualTo(25);
    }

    @Test
    void importAreas_climbWithUnmatchedGrade_leavesPitchGradeNullAndCountsIt() {
        OpenBetaClimbDto climb = climb(
                "climb-2",
                "Vía sin grado sembrado",
                new OpenBetaClimbTypeDto(true, false, false, false, false, false),
                new OpenBetaGradesDto("9c", null, null, null, null, null),
                null,
                null);
        OpenBetaAreaDto crag = leafArea("crag-2", "Crag sin sembrar", List.of(), null, null, List.of(climb));
        when(gradingSystemRepository.findByName("French")).thenReturn(Optional.empty());

        OpenBetaImportResult result = importService.importAreas(List.of(crag));

        assertThat(result.gradesUnmatched()).isEqualTo(1);
        ArgumentCaptor<Pitch> pitchCaptor = ArgumentCaptor.forClass(Pitch.class);
        org.mockito.Mockito.verify(pitchRepository).save(pitchCaptor.capture());
        assertThat(pitchCaptor.getValue().getGrade()).isNull();
    }

    @Test
    void importAreas_boulderingOnlyClimb_resolvesToBoulderType() {
        OpenBetaClimbDto climb = climb(
                "climb-3",
                "Bloque",
                new OpenBetaClimbTypeDto(false, false, true, false, false, false),
                null,
                "Desconocido",
                null);
        OpenBetaAreaDto crag = leafArea("crag-3", "Zona de bloque", List.of(), null, null, List.of(climb));

        importService.importAreas(List.of(crag));

        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        org.mockito.Mockito.verify(routeRepository).save(routeCaptor.capture());
        assertThat(routeCaptor.getValue().getRouteType()).isEqualTo(Route.RouteType.BOULDER);
        assertThat(routeCaptor.getValue().getFirstAscentBy()).isEqualTo("Desconocido");
        assertThat(routeCaptor.getValue().getFirstAscentDate()).isNull();
    }

    @Test
    void preview_flagsMissingNameUnmatchedGradeAndDuplicateCoordinates() {
        OpenBetaClimbDto unnamedClimb = new OpenBetaClimbDto(
                "climb-4",
                null,
                null,
                -1,
                new OpenBetaClimbTypeDto(false, false, true, false, false, false),
                new OpenBetaGradesDto(null, null, "V99", null, null, null),
                new OpenBetaClimbMetadataDto(36.9123, -4.7501));
        OpenBetaAreaDto crag = leafArea("crag-4", "Crag con problemas", List.of(), 36.9123, -4.7501, List.of(unnamedClimb));
        when(gradingSystemRepository.findByName("V-Scale")).thenReturn(Optional.empty());

        List<OpenBetaPreviewRow> rows = importService.preview(List.of(crag));

        assertThat(rows).hasSize(2);
        OpenBetaPreviewRow routeRow = rows.get(1);
        assertThat(routeRow.level()).isEqualTo("ROUTE");
        assertThat(routeRow.issues())
                .contains("MISSING_NAME")
                .contains("UNMATCHED_GRADE")
                .contains("DUPLICATE_COORDS_WITH_PARENT")
                .contains("LENGTH_UNKNOWN");
        assertThat(routeRow.gradeMatched()).isFalse();
        assertThat(routeRow.lengthM()).isNull();

        verifyNoInteractions(routeRepository, pitchRepository, cragRepository, sectorRepository);
    }

    @Test
    void preview_matchedGradeAndDistinctCoordinates_noIssues() {
        OpenBetaClimbDto climb = climb(
                "climb-5",
                "Vía Limpia",
                new OpenBetaClimbTypeDto(true, false, false, false, false, false),
                new OpenBetaGradesDto("6a", null, null, null, null, null),
                "J. García, 1998",
                25);
        OpenBetaAreaDto crag = leafArea("crag-5", "Crag Limpio", List.of(), 36.9123, -4.7501, List.of(climb));
        GradingSystem french = new GradingSystem();
        french.setId(10L);
        Grade grade6a = new Grade();
        grade6a.setId(99L);
        when(gradingSystemRepository.findByName("French")).thenReturn(Optional.of(french));
        when(gradeRepository.findByGradingSystemIdAndLabel(10L, "6a")).thenReturn(Optional.of(grade6a));

        List<OpenBetaPreviewRow> rows = importService.preview(List.of(crag));

        OpenBetaPreviewRow routeRow = rows.get(1);
        assertThat(routeRow.issues()).isEmpty();
        assertThat(routeRow.gradeMatched()).isTrue();
        assertThat(routeRow.gradeSystem()).isEqualTo("French");
        assertThat(routeRow.gradeLabel()).isEqualTo("6a");
        assertThat(routeRow.lengthM()).isEqualTo(25);
        assertThat(routeRow.firstAscentBy()).isEqualTo("J. García");
        assertThat(routeRow.firstAscentDate()).isEqualTo(LocalDate.of(1998, 1, 1));
    }

    @Test
    void importAreas_existingEntities_updatesInsteadOfCreating() {
        Zone existingZone = new Zone();
        existingZone.setId(5L);
        existingZone.setExternalSource(OpenBetaImportService.EXTERNAL_SOURCE);
        existingZone.setExternalId("zone-1");
        when(zoneRepository.findByExternalSourceAndExternalId(OpenBetaImportService.EXTERNAL_SOURCE, "zone-1"))
                .thenReturn(Optional.of(existingZone));

        OpenBetaImportResult result = importService.importAreas(List.of(zoneArea("zone-1", "Andalucía", List.of())));

        assertThat(result.zonesCreated()).isZero();
        assertThat(result.zonesUpdated()).isEqualTo(1);
    }
}
