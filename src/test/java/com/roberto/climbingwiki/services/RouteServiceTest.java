package com.roberto.climbingwiki.services;

import org.springframework.data.jpa.domain.Specification;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.security.AuthenticatedUser;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.dtos.RouteRequest;
import com.roberto.climbingwiki.dtos.RouteResponse;
import com.roberto.climbingwiki.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.exceptions.SectorNotFoundException;
import com.roberto.climbingwiki.exceptions.TagNotFoundException;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Sector;
import com.roberto.climbingwiki.model.entities.Tag;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.PitchRepository;
import com.roberto.climbingwiki.repositories.PitchSummaryRow;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.repositories.SectorRepository;
import com.roberto.climbingwiki.repositories.TagRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    private static final AuthenticatedUser REQUESTER =
            new AuthenticatedUser(1L, "climber", User.Role.USER);

    @Mock
    private CatalogAccess access;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private PitchRepository pitchRepository;

    private RouteService routeService;

    @BeforeEach
    void setUp() {
        routeService = new RouteService(routeRepository, sectorRepository, tagRepository, pitchRepository, access);
    }

    private Sector sector(Long id) {
        Sector sector = new Sector();
        sector.setId(id);
        return sector;
    }

    private Route route(Long id, Sector sector, String name) {
        Route route = new Route();
        route.setId(id);
        route.setSector(sector);
        route.setName(name);
        route.setRouteType(Route.RouteType.SPORT);
        return route;
    }

    private RouteRequest request(Long sectorId, Set<Long> tagIds) {
        return new RouteRequest(sectorId, "La Rambla", Route.RouteType.SPORT, "desc", "notes",
                "John Doe", null, null, null, tagIds);
    }

    @Test
    void search_returnsMappedResponses() {
        Sector sector = sector(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Route> routePage = new PageImpl<>(
                List.of(route(1L, sector, "La Rambla"), route(2L, sector, "Bla")), pageable, 2);
        when(routeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Route>>any(), eq(pageable)))
                .thenReturn(routePage);

        Page<RouteResponse> result = routeService.search(null, null, pageable);

        assertThat(result.getContent()).extracting(RouteResponse::name).containsExactly("La Rambla", "Bla");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void search_includesHardestGradeAndTotalLength_withOneSummaryQueryPerPage() {
        Sector sector = sector(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Route> routePage = new PageImpl<>(
                List.of(route(1L, sector, "La Rambla"), route(2L, sector, "Bla")), pageable, 2);
        when(routeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Route>>any(), eq(pageable)))
                .thenReturn(routePage);
        when(pitchRepository.findSummaryRows(List.of(1L, 2L))).thenReturn(List.of(
                new PitchSummaryRow(1L, 10L, 50, 20),
                new PitchSummaryRow(1L, 12L, 70, 25)));

        Page<RouteResponse> result = routeService.search(null, null, pageable);

        RouteResponse rambla = result.getContent().get(0);
        RouteResponse bla = result.getContent().get(1);
        assertThat(rambla.hardestGradeId()).isEqualTo(12L);
        assertThat(rambla.totalLengthM()).isEqualTo(45);
        assertThat(bla.hardestGradeId()).isNull();
        assertThat(bla.totalLengthM()).isNull();
        verify(pitchRepository).findSummaryRows(List.of(1L, 2L));
    }

    @Test
    void findById_includesRouteSummary() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L, sector(1L), "La Rambla")));
        when(pitchRepository.findSummaryRows(List.of(1L))).thenReturn(List.of(new PitchSummaryRow(1L, 12L, 70, 25)));

        RouteResponse result = routeService.findById(1L);

        assertThat(result.hardestGradeId()).isEqualTo(12L);
        assertThat(result.totalLengthM()).isEqualTo(25);
    }

    @Test
    void findById_existingRoute_returnsResponse() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L, sector(1L), "La Rambla")));

        RouteResponse result = routeService.findById(1L);

        assertThat(result.name()).isEqualTo("La Rambla");
    }

    @Test
    void findById_missingRoute_throws() {
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.findById(99L))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_missingSector_throws() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.create(request(1L, null), REQUESTER))
                .isInstanceOf(SectorNotFoundException.class)
                .hasMessageContaining("1");
    }


    @Test
    void create_missingTag_throws() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector(1L)));
        when(tagRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(new Tag()));

        assertThatThrownBy(() -> routeService.create(request(1L, Set.of(1L, 2L)), REQUESTER))
                .isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void create_withValidSectorAndTags_savesRouteAndStampsCreator() {
        Sector sector = sector(1L);
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("classic");

        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(tagRepository.findAllById(Set.of(1L))).thenReturn(List.of(tag));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteResponse result = routeService.create(request(1L, Set.of(1L)), REQUESTER);

        assertThat(result.name()).isEqualTo("La Rambla");
        assertThat(result.sectorId()).isEqualTo(1L);
        verify(access).stampCreation(any(Route.class), eq(REQUESTER));
        assertThat(result.tags()).containsExactly("classic");
    }

    @Test
    void create_withNoTagIds_resultsInEmptyTagSet() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector(1L)));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteResponse result = routeService.create(request(1L, null), REQUESTER);

        assertThat(result.tags()).isEmpty();
        verify(tagRepository, never()).findAllById(any());
    }

    @Test
    void update_existingRoute_appliesChanges() {
        Route existing = route(1L, sector(1L), "Old name");
        when(routeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector(1L)));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteResponse result = routeService.update(1L, request(1L, null), REQUESTER);

        assertThat(result.name()).isEqualTo("La Rambla");
    }

    @Test
    void update_missingRoute_throws() {
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.update(99L, request(1L, null), REQUESTER))
                .isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void delete_existingRoute_deletes() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(new Route()));

        routeService.delete(1L, REQUESTER);

        verify(routeRepository).deleteById(1L);
    }

    @Test
    void delete_missingRoute_throwsAndDoesNotDelete() {
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.delete(99L, REQUESTER)).isInstanceOf(RouteNotFoundException.class);

        verify(routeRepository, never()).deleteById(99L);
    }

    // ----- access rules (CatalogAccess is mocked; its own rules are covered by CatalogAccessTest)

    @Test
    void update_whenAccessDenied_throwsAndDoesNotSave() {
        Route existing = new Route();
        existing.setId(1L);
        when(routeRepository.findById(1L)).thenReturn(Optional.of(existing));
        doThrow(new ForbiddenException()).when(access).checkCanEdit(existing, REQUESTER);

        assertThatThrownBy(() -> routeService.update(1L, request(1L, null), REQUESTER)).isInstanceOf(ForbiddenException.class);

        verify(routeRepository, never()).save(any());
    }

    @Test
    void delete_passesChildDataFlagToAccessRules() {
        Route existing = new Route();
        existing.setId(1L);
        when(routeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(pitchRepository.existsByRouteId(1L)).thenReturn(true);

        routeService.delete(1L, REQUESTER);

        verify(access).checkCanDelete(existing, REQUESTER, true);
        verify(routeRepository).deleteById(1L);
    }

    @Test
    void delete_whenAccessDenied_doesNotDelete() {
        Route existing = new Route();
        existing.setId(1L);
        when(routeRepository.findById(1L)).thenReturn(Optional.of(existing));
        doThrow(new ForbiddenException()).when(access).checkCanDelete(any(), eq(REQUESTER), anyBoolean());

        assertThatThrownBy(() -> routeService.delete(1L, REQUESTER)).isInstanceOf(ForbiddenException.class);

        verify(routeRepository, never()).deleteById(any());
    }

    @Test
    void setVerified_appliesAccessRulesAndSaves() {
        Route existing = new Route();
        existing.setId(1L);
        Sector parent = new Sector();
        parent.setId(9L);
        existing.setSector(parent);
        when(routeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(routeRepository.save(existing)).thenReturn(existing);

        routeService.setVerified(1L, true, REQUESTER);

        verify(access).setVerified(existing, true, REQUESTER);
        verify(routeRepository).save(existing);
    }
}
