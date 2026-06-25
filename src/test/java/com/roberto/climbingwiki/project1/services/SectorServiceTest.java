package com.roberto.climbingwiki.project1.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.project1.dtos.SectorRequest;
import com.roberto.climbingwiki.project1.dtos.SectorResponse;
import com.roberto.climbingwiki.project1.exceptions.CragNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.SectorNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.Crag;
import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.model.entities.Sector;
import com.roberto.climbingwiki.project1.repositories.CragRepository;
import com.roberto.climbingwiki.project1.repositories.RouteRepository;
import com.roberto.climbingwiki.project1.repositories.SectorRepository;
import java.util.List;
import java.util.Optional;
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
class SectorServiceTest {

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private CragRepository cragRepository;

    @Mock
    private RouteRepository routeRepository;

    private SectorService sectorService;

    @BeforeEach
    void setUp() {
        sectorService = new SectorService(sectorRepository, cragRepository, routeRepository);
    }

    private RouteRepository.SectorRouteType routeTypeRow(Long sectorId, Route.RouteType type) {
        return new RouteRepository.SectorRouteType() {
            @Override
            public Long getSectorId() {
                return sectorId;
            }

            @Override
            public Route.RouteType getRouteType() {
                return type;
            }
        };
    }

    private Sector sector(Long id, Crag crag, String name) {
        Sector sector = new Sector();
        sector.setId(id);
        sector.setCrag(crag);
        sector.setName(name);
        return sector;
    }

    private Crag crag(Long id) {
        Crag crag = new Crag();
        crag.setId(id);
        return crag;
    }

    @Test
    void findAll_returnsMappedResponses() {
        Crag crag = crag(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Sector> sectorPage = new PageImpl<>(
                List.of(sector(1L, crag, "Pared de Sant Jeroni"), sector(2L, crag, "Sector Diedres")), pageable, 2);
        when(sectorRepository.findAll(pageable)).thenReturn(sectorPage);

        Page<SectorResponse> result = sectorService.findAll(pageable);

        assertThat(result.getContent()).extracting(SectorResponse::name)
                .containsExactly("Pared de Sant Jeroni", "Sector Diedres");
    }

    @Test
    void findAll_includesDistinctRouteTypesPerSector() {
        Crag crag = crag(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Sector> sectorPage = new PageImpl<>(
                List.of(sector(1L, crag, "Pared de Sant Jeroni"), sector(2L, crag, "Sector Diedres")), pageable, 2);
        when(sectorRepository.findAll(pageable)).thenReturn(sectorPage);
        when(routeRepository.findDistinctRouteTypesBySectorIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(
                        routeTypeRow(1L, Route.RouteType.SPORT),
                        routeTypeRow(1L, Route.RouteType.TRAD),
                        routeTypeRow(2L, Route.RouteType.BOULDER)));

        Page<SectorResponse> result = sectorService.findAll(pageable);

        assertThat(result.getContent().get(0).routeTypes())
                .containsExactlyInAnyOrder(Route.RouteType.SPORT, Route.RouteType.TRAD);
        assertThat(result.getContent().get(1).routeTypes()).containsExactly(Route.RouteType.BOULDER);
    }

    @Test
    void findAll_sectorWithNoRoutes_returnsEmptyRouteTypes() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Sector> sectorPage = new PageImpl<>(List.of(sector(1L, crag(1L), "Pared de Sant Jeroni")), pageable, 1);
        when(sectorRepository.findAll(pageable)).thenReturn(sectorPage);
        when(routeRepository.findDistinctRouteTypesBySectorIdIn(List.of(1L))).thenReturn(List.of());

        Page<SectorResponse> result = sectorService.findAll(pageable);

        assertThat(result.getContent().get(0).routeTypes()).isEmpty();
    }

    @Test
    void findByCragId_returnsMappedResponses() {
        Crag crag = crag(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Sector> sectorPage = new PageImpl<>(List.of(sector(1L, crag, "Pared de Sant Jeroni")), pageable, 1);
        when(sectorRepository.findByCragId(1L, pageable)).thenReturn(sectorPage);

        Page<SectorResponse> result = sectorService.findByCragId(1L, pageable);

        assertThat(result.getContent()).extracting(SectorResponse::name).containsExactly("Pared de Sant Jeroni");
    }

    @Test
    void findById_existingSector_returnsResponse() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector(1L, crag(1L), "Pared de Sant Jeroni")));

        SectorResponse result = sectorService.findById(1L);

        assertThat(result.name()).isEqualTo("Pared de Sant Jeroni");
        assertThat(result.cragId()).isEqualTo(1L);
    }

    @Test
    void findById_missingSector_throws() {
        when(sectorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectorService.findById(99L))
                .isInstanceOf(SectorNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_resolvesCragAndSaves() {
        when(cragRepository.findById(1L)).thenReturn(Optional.of(crag(1L)));
        when(sectorRepository.save(any(Sector.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SectorResponse result = sectorService.create(new SectorRequest(1L, "New Sector", "desc", null, null));

        assertThat(result.name()).isEqualTo("New Sector");
        assertThat(result.cragId()).isEqualTo(1L);
    }

    @Test
    void create_missingCrag_throws() {
        when(cragRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectorService.create(new SectorRequest(99L, "New Sector", "desc", null, null)))
                .isInstanceOf(CragNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_existingSector_appliesChanges() {
        Sector existing = sector(1L, crag(1L), "Old name");
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cragRepository.findById(1L)).thenReturn(Optional.of(crag(1L)));
        when(sectorRepository.save(any(Sector.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SectorResponse result = sectorService.update(1L, new SectorRequest(1L, "New name", "desc", null, null));

        assertThat(result.name()).isEqualTo("New name");
    }

    @Test
    void update_missingSector_throws() {
        when(sectorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectorService.update(99L, new SectorRequest(1L, "name", "desc", null, null)))
                .isInstanceOf(SectorNotFoundException.class);
    }

    @Test
    void delete_existingSector_deletes() {
        when(sectorRepository.existsById(1L)).thenReturn(true);

        sectorService.delete(1L);

        verify(sectorRepository).deleteById(1L);
    }

    @Test
    void delete_missingSector_throwsAndDoesNotDelete() {
        when(sectorRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> sectorService.delete(99L)).isInstanceOf(SectorNotFoundException.class);

        verify(sectorRepository, never()).deleteById(99L);
    }
}
