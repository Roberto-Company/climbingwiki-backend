package com.roberto.climbingwiki.project1.services;

import org.springframework.data.jpa.domain.Specification;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.roberto.climbingwiki.project1.repositories.PitchRepository;
import com.roberto.climbingwiki.project1.repositories.PitchSummaryRow;
import com.roberto.climbingwiki.project1.repositories.RouteRepository;
import com.roberto.climbingwiki.project1.repositories.SectorRepository;
import com.roberto.climbingwiki.project1.repositories.TagRepository;
import com.roberto.climbingwiki.project1.repositories.UserRepository;
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

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private PitchRepository pitchRepository;

    private RouteService routeService;

    @BeforeEach
    void setUp() {
        routeService = new RouteService(routeRepository, sectorRepository, userRepository, tagRepository, pitchRepository);
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

    private RouteRequest request(Long sectorId, Long createdById, Set<Long> tagIds) {
        return new RouteRequest(sectorId, createdById, "La Rambla", Route.RouteType.SPORT, "desc", "notes",
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

        assertThatThrownBy(() -> routeService.create(request(1L, null, null)))
                .isInstanceOf(SectorNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void create_missingCreatedBy_throws() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector(1L)));
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.create(request(1L, 5L, null)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("5");
    }

    @Test
    void create_missingTag_throws() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector(1L)));
        when(tagRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(new Tag()));

        assertThatThrownBy(() -> routeService.create(request(1L, null, Set.of(1L, 2L))))
                .isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void create_withValidSectorUserAndTags_savesRoute() {
        Sector sector = sector(1L);
        User user = new User();
        user.setId(5L);
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("classic");

        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(tagRepository.findAllById(Set.of(1L))).thenReturn(List.of(tag));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteResponse result = routeService.create(request(1L, 5L, Set.of(1L)));

        assertThat(result.name()).isEqualTo("La Rambla");
        assertThat(result.sectorId()).isEqualTo(1L);
        assertThat(result.createdById()).isEqualTo(5L);
        assertThat(result.tags()).containsExactly("classic");
    }

    @Test
    void create_withNoTagIds_resultsInEmptyTagSet() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector(1L)));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteResponse result = routeService.create(request(1L, null, null));

        assertThat(result.tags()).isEmpty();
        verify(tagRepository, never()).findAllById(any());
    }

    @Test
    void update_existingRoute_appliesChanges() {
        Route existing = route(1L, sector(1L), "Old name");
        when(routeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector(1L)));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteResponse result = routeService.update(1L, request(1L, null, null));

        assertThat(result.name()).isEqualTo("La Rambla");
    }

    @Test
    void update_missingRoute_throws() {
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.update(99L, request(1L, null, null)))
                .isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void delete_existingRoute_deletes() {
        when(routeRepository.existsById(1L)).thenReturn(true);

        routeService.delete(1L);

        verify(routeRepository).deleteById(1L);
    }

    @Test
    void delete_missingRoute_throwsAndDoesNotDelete() {
        when(routeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> routeService.delete(99L)).isInstanceOf(RouteNotFoundException.class);

        verify(routeRepository, never()).deleteById(99L);
    }
}
