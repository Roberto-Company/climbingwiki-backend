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
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.dtos.ZoneRequest;
import com.roberto.climbingwiki.dtos.ZoneResponse;
import com.roberto.climbingwiki.exceptions.InvalidZoneParentException;
import com.roberto.climbingwiki.exceptions.ZoneNotFoundException;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Zone;
import com.roberto.climbingwiki.repositories.CragRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.repositories.ZoneRepository;
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

    private static final AuthenticatedUser REQUESTER =
            new AuthenticatedUser(1L, "climber", User.Role.USER);

    @Mock
    private CatalogAccess access;

    @Mock
    private CragRepository cragRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private RouteRepository routeRepository;

    private ZoneService zoneService;

    @BeforeEach
    void setUp() {
        zoneService = new ZoneService(zoneRepository, routeRepository, cragRepository, access);
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
    void search_returnsMappedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Zone> zonePage = new PageImpl<>(List.of(zone(1L, "Montserrat"), zone(2L, "Siurana")), pageable, 2);
        when(zoneRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Zone>>any(), eq(pageable)))
                .thenReturn(zonePage);

        Page<ZoneResponse> result = zoneService.search(null, true, "s", pageable);

        assertThat(result.getContent()).extracting(ZoneResponse::name).containsExactly("Montserrat", "Siurana");
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

        ZoneResponse result = zoneService.create(request(null), REQUESTER);

        assertThat(result.parentZoneId()).isNull();
        verify(zoneRepository, never()).findById(any());
    }

    @Test
    void create_withParentId_resolvesParent() {
        Zone parent = zone(7L, "Parent");
        when(zoneRepository.findById(7L)).thenReturn(Optional.of(parent));
        when(zoneRepository.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ZoneResponse result = zoneService.create(request(7L), REQUESTER);

        assertThat(result.parentZoneId()).isEqualTo(7L);
    }

    @Test
    void create_withMissingParentId_throws() {
        when(zoneRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zoneService.create(request(7L), REQUESTER))
                .isInstanceOf(ZoneNotFoundException.class)
                .hasMessageContaining("7");
    }

    @Test
    void update_existingZone_appliesChanges() {
        Zone existing = zone(1L, "Old name");
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(zoneRepository.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ZoneResponse result = zoneService.update(1L, request(null), REQUESTER);

        assertThat(result.name()).isEqualTo("Montserrat");
    }

    @Test
    void update_missingZone_throws() {
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zoneService.update(99L, request(null), REQUESTER))
                .isInstanceOf(ZoneNotFoundException.class);
    }

    @Test
    void update_selfAsParent_throwsInvalidZoneParent() {
        Zone existing = zone(1L, "Montserrat");
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> zoneService.update(1L, request(1L), REQUESTER))
                .isInstanceOf(InvalidZoneParentException.class)
                .hasMessageContaining("1");

        verify(zoneRepository, never()).save(any());
    }

    @Test
    void delete_existingZone_deletes() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(new Zone()));

        zoneService.delete(1L, REQUESTER);

        verify(zoneRepository).deleteById(1L);
    }

    @Test
    void delete_missingZone_throwsAndDoesNotDelete() {
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zoneService.delete(99L, REQUESTER)).isInstanceOf(ZoneNotFoundException.class);

        verify(zoneRepository, never()).deleteById(99L);
    }

    // ----- access rules (CatalogAccess is mocked; its own rules are covered by CatalogAccessTest)

    @Test
    void update_whenAccessDenied_throwsAndDoesNotSave() {
        Zone existing = new Zone();
        existing.setId(1L);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(existing));
        doThrow(new ForbiddenException()).when(access).checkCanEdit(existing, REQUESTER);

        assertThatThrownBy(() -> zoneService.update(1L, request(null), REQUESTER)).isInstanceOf(ForbiddenException.class);

        verify(zoneRepository, never()).save(any());
    }

    @Test
    void delete_passesChildDataFlagToAccessRules() {
        Zone existing = new Zone();
        existing.setId(1L);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cragRepository.existsByZoneId(1L)).thenReturn(true);

        zoneService.delete(1L, REQUESTER);

        verify(access).checkCanDelete(existing, REQUESTER, true);
        verify(zoneRepository).deleteById(1L);
    }

    @Test
    void delete_whenAccessDenied_doesNotDelete() {
        Zone existing = new Zone();
        existing.setId(1L);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(existing));
        doThrow(new ForbiddenException()).when(access).checkCanDelete(any(), eq(REQUESTER), anyBoolean());

        assertThatThrownBy(() -> zoneService.delete(1L, REQUESTER)).isInstanceOf(ForbiddenException.class);

        verify(zoneRepository, never()).deleteById(any());
    }

    @Test
    void setVerified_appliesAccessRulesAndSaves() {
        Zone existing = new Zone();
        existing.setId(1L);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(zoneRepository.save(existing)).thenReturn(existing);

        zoneService.setVerified(1L, true, REQUESTER);

        verify(access).setVerified(existing, true, REQUESTER);
        verify(zoneRepository).save(existing);
    }
}
