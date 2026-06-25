package com.roberto.climbingwiki.project1.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.project1.dtos.ZoneRequest;
import com.roberto.climbingwiki.project1.dtos.ZoneResponse;
import com.roberto.climbingwiki.project1.exceptions.InvalidZoneParentException;
import com.roberto.climbingwiki.project1.exceptions.ZoneNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.model.entities.Zone;
import com.roberto.climbingwiki.project1.repositories.RouteRepository;
import com.roberto.climbingwiki.project1.repositories.ZoneRepository;
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
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private RouteRepository routeRepository;

    private ZoneService zoneService;

    @BeforeEach
    void setUp() {
        zoneService = new ZoneService(zoneRepository, routeRepository);
    }

    private Zone zone(Long id, String name) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setName(name);
        return zone;
    }

    private ZoneRequest request(Long parentZoneId) {
        return new ZoneRequest(parentZoneId, "Montserrat", "desc");
    }

    @Test
    void findAll_returnsMappedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Zone> zonePage = new PageImpl<>(List.of(zone(1L, "Montserrat"), zone(2L, "Siurana")), pageable, 2);
        when(zoneRepository.findAll(pageable)).thenReturn(zonePage);

        Page<ZoneResponse> result = zoneService.findAll(pageable);

        assertThat(result.getContent()).extracting(ZoneResponse::name).containsExactly("Montserrat", "Siurana");
    }

    @Test
    void findByParentZoneId_returnsMappedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Zone> zonePage = new PageImpl<>(List.of(zone(2L, "Sant Jeroni")), pageable, 1);
        when(zoneRepository.findByParentZoneId(1L, pageable)).thenReturn(zonePage);

        Page<ZoneResponse> result = zoneService.findByParentZoneId(1L, pageable);

        assertThat(result.getContent()).extracting(ZoneResponse::name).containsExactly("Sant Jeroni");
    }

    @Test
    void findRootZones_returnsMappedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Zone> zonePage = new PageImpl<>(List.of(zone(1L, "Montserrat")), pageable, 1);
        when(zoneRepository.findByParentZoneIsNull(pageable)).thenReturn(zonePage);

        Page<ZoneResponse> result = zoneService.findRootZones(pageable);

        assertThat(result.getContent()).extracting(ZoneResponse::name).containsExactly("Montserrat");
    }

    @Test
    void findById_existingZone_returnsResponse() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone(1L, "Montserrat")));

        ZoneResponse result = zoneService.findById(1L);

        assertThat(result.name()).isEqualTo("Montserrat");
    }

    @Test
    void findById_missingZone_throws() {
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zoneService.findById(99L))
                .isInstanceOf(ZoneNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findById_includesRouteTypesFromZoneItself() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone(1L, "Montserrat")));
        when(routeRepository.findDistinctRouteTypesByZoneIdIn(Set.of(1L)))
                .thenReturn(List.of(Route.RouteType.SPORT, Route.RouteType.TRAD));

        ZoneResponse result = zoneService.findById(1L);

        assertThat(result.routeTypes()).containsExactlyInAnyOrder(Route.RouteType.SPORT, Route.RouteType.TRAD);
    }

    @Test
    void findById_includesRouteTypesFromDescendantZones() {
        Zone child = zone(2L, "Sant Jeroni");
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone(1L, "Montserrat")));
        when(zoneRepository.findByParentZoneId(1L)).thenReturn(List.of(child));
        when(routeRepository.findDistinctRouteTypesByZoneIdIn(Set.of(1L, 2L)))
                .thenReturn(List.of(Route.RouteType.BOULDER));

        ZoneResponse result = zoneService.findById(1L);

        assertThat(result.routeTypes()).containsExactly(Route.RouteType.BOULDER);
    }

    @Test
    void findById_zoneWithNoRoutes_returnsEmptyRouteTypes() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone(1L, "Montserrat")));

        ZoneResponse result = zoneService.findById(1L);

        assertThat(result.routeTypes()).isEmpty();
    }

    @Test
    void create_withoutParent_savesWithNullParent() {
        when(zoneRepository.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ZoneResponse result = zoneService.create(request(null));

        assertThat(result.parentZoneId()).isNull();
        verify(zoneRepository, never()).findById(any());
    }

    @Test
    void create_withParentId_resolvesParent() {
        Zone parent = zone(7L, "Parent");
        when(zoneRepository.findById(7L)).thenReturn(Optional.of(parent));
        when(zoneRepository.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ZoneResponse result = zoneService.create(request(7L));

        assertThat(result.parentZoneId()).isEqualTo(7L);
    }

    @Test
    void create_withMissingParentId_throws() {
        when(zoneRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zoneService.create(request(7L)))
                .isInstanceOf(ZoneNotFoundException.class)
                .hasMessageContaining("7");
    }

    @Test
    void update_existingZone_appliesChanges() {
        Zone existing = zone(1L, "Old name");
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(zoneRepository.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ZoneResponse result = zoneService.update(1L, request(null));

        assertThat(result.name()).isEqualTo("Montserrat");
    }

    @Test
    void update_missingZone_throws() {
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zoneService.update(99L, request(null)))
                .isInstanceOf(ZoneNotFoundException.class);
    }

    @Test
    void update_selfAsParent_throwsInvalidZoneParent() {
        Zone existing = zone(1L, "Montserrat");
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> zoneService.update(1L, request(1L)))
                .isInstanceOf(InvalidZoneParentException.class)
                .hasMessageContaining("1");

        verify(zoneRepository, never()).save(any());
    }

    @Test
    void delete_existingZone_deletes() {
        when(zoneRepository.existsById(1L)).thenReturn(true);

        zoneService.delete(1L);

        verify(zoneRepository).deleteById(1L);
    }

    @Test
    void delete_missingZone_throwsAndDoesNotDelete() {
        when(zoneRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> zoneService.delete(99L)).isInstanceOf(ZoneNotFoundException.class);

        verify(zoneRepository, never()).deleteById(99L);
    }
}
