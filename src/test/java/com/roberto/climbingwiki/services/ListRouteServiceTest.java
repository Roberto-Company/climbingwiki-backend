package com.roberto.climbingwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.dtos.ListRouteRequest;
import com.roberto.climbingwiki.dtos.ListRouteResponse;
import com.roberto.climbingwiki.dtos.UpsertResult;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.ListRouteNotFoundException;
import com.roberto.climbingwiki.exceptions.RouteListNotFoundException;
import com.roberto.climbingwiki.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.model.entities.ListRoute;
import com.roberto.climbingwiki.model.entities.ListRouteId;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.RouteList;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.ListRouteRepository;
import com.roberto.climbingwiki.repositories.RouteListRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListRouteServiceTest {

    @Mock
    private ListRouteRepository listRouteRepository;

    @Mock
    private RouteListRepository routeListRepository;

    @Mock
    private RouteRepository routeRepository;

    private ListRouteService listRouteService;

    @BeforeEach
    void setUp() {
        listRouteService = new ListRouteService(listRouteRepository, routeListRepository, routeRepository);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private RouteList routeList(Long id) {
        RouteList routeList = new RouteList();
        routeList.setId(id);
        routeList.setUser(user(1L));
        return routeList;
    }

    private Route route(Long id) {
        Route route = new Route();
        route.setId(id);
        return route;
    }

    private ListRoute listRoute(Long listId, Long routeId) {
        ListRoute listRoute = new ListRoute();
        listRoute.setList(routeList(listId));
        listRoute.setRoute(route(routeId));
        return listRoute;
    }

    private ListRouteRequest request(Long listId, Long routeId) {
        return new ListRouteRequest(listId, routeId);
    }

    private AuthenticatedUser self(Long id) {
        return new AuthenticatedUser(id, "jane", User.Role.USER);
    }

    private AuthenticatedUser admin(Long id) {
        return new AuthenticatedUser(id, "boss", User.Role.ADMIN);
    }

    @Test
    void findAll_returnsMappedResponses() {
        when(listRouteRepository.findAll()).thenReturn(List.of(listRoute(1L, 10L), listRoute(1L, 20L)));

        List<ListRouteResponse> result = listRouteService.findAll();

        assertThat(result).extracting(ListRouteResponse::routeId).containsExactly(10L, 20L);
    }

    @Test
    void findByList_returnsRoutesInThatList() {
        when(listRouteRepository.findByListId(1L)).thenReturn(List.of(listRoute(1L, 10L)));

        List<ListRouteResponse> result = listRouteService.findByList(1L);

        assertThat(result).extracting(ListRouteResponse::listId).containsExactly(1L);
    }

    @Test
    void findByRoute_returnsListsContainingThatRoute() {
        when(listRouteRepository.findByRouteId(10L)).thenReturn(List.of(listRoute(1L, 10L)));

        List<ListRouteResponse> result = listRouteService.findByRoute(10L);

        assertThat(result).extracting(ListRouteResponse::routeId).containsExactly(10L);
    }

    @Test
    void findOne_existingListRoute_returnsResponse() {
        when(listRouteRepository.findById(new ListRouteId(1L, 10L))).thenReturn(Optional.of(listRoute(1L, 10L)));

        ListRouteResponse result = listRouteService.findOne(1L, 10L);

        assertThat(result.routeId()).isEqualTo(10L);
    }

    @Test
    void findOne_missingListRoute_throws() {
        when(listRouteRepository.findById(new ListRouteId(1L, 99L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listRouteService.findOne(1L, 99L))
                .isInstanceOf(ListRouteNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void upsert_missingList_throws() {
        when(routeListRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listRouteService.upsert(request(1L, 10L), self(1L)))
                .isInstanceOf(RouteListNotFoundException.class);

        verify(listRouteRepository, never()).save(any());
    }

    @Test
    void upsert_missingRoute_throws() {
        when(listRouteRepository.existsById(any())).thenReturn(false);
        when(listRouteRepository.findById(any())).thenReturn(Optional.empty());
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(routeList(1L)));
        when(routeRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listRouteService.upsert(request(1L, 10L), self(1L)))
                .isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void upsert_newListRoute_createsAndReturnsCreatedTrue() {
        when(listRouteRepository.existsById(new ListRouteId(1L, 10L))).thenReturn(false);
        when(listRouteRepository.findById(new ListRouteId(1L, 10L))).thenReturn(Optional.empty());
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(routeList(1L)));
        when(routeRepository.findById(10L)).thenReturn(Optional.of(route(10L)));
        when(listRouteRepository.save(any(ListRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpsertResult<ListRouteResponse> result = listRouteService.upsert(request(1L, 10L), self(1L));

        assertThat(result.created()).isTrue();
        assertThat(result.data().routeId()).isEqualTo(10L);
    }

    @Test
    void upsert_existingListRoute_isNoOpAndReturnsCreatedFalse() {
        ListRoute existing = listRoute(1L, 10L);
        when(listRouteRepository.existsById(new ListRouteId(1L, 10L))).thenReturn(true);
        when(listRouteRepository.findById(new ListRouteId(1L, 10L))).thenReturn(Optional.of(existing));
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(routeList(1L)));
        when(routeRepository.findById(10L)).thenReturn(Optional.of(route(10L)));
        when(listRouteRepository.save(any(ListRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpsertResult<ListRouteResponse> result = listRouteService.upsert(request(1L, 10L), self(1L));

        assertThat(result.created()).isFalse();
    }

    @Test
    void upsert_byAnotherRegularUser_throwsForbidden() {
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(routeList(1L)));

        assertThatThrownBy(() -> listRouteService.upsert(request(1L, 10L), self(2L)))
                .isInstanceOf(ForbiddenException.class);

        verify(listRouteRepository, never()).save(any());
    }

    @Test
    void upsert_byModerator_succeeds() {
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(routeList(1L)));
        when(listRouteRepository.existsById(new ListRouteId(1L, 10L))).thenReturn(false);
        when(listRouteRepository.findById(new ListRouteId(1L, 10L))).thenReturn(Optional.empty());
        when(routeRepository.findById(10L)).thenReturn(Optional.of(route(10L)));
        when(listRouteRepository.save(any(ListRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpsertResult<ListRouteResponse> result = listRouteService.upsert(
                request(1L, 10L), new AuthenticatedUser(99L, "mod", User.Role.MODERATOR));

        assertThat(result.data().routeId()).isEqualTo(10L);
    }

    @Test
    void delete_existingListRoute_deletes() {
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(routeList(1L)));
        when(listRouteRepository.existsById(new ListRouteId(1L, 10L))).thenReturn(true);

        listRouteService.delete(1L, 10L, self(1L));

        verify(listRouteRepository).deleteById(new ListRouteId(1L, 10L));
    }

    @Test
    void delete_missingListRoute_isNoOpAndDoesNotThrow() {
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(routeList(1L)));
        when(listRouteRepository.existsById(new ListRouteId(1L, 99L))).thenReturn(false);

        listRouteService.delete(1L, 99L, self(1L));

        verify(listRouteRepository, never()).deleteById(any());
    }

    @Test
    void delete_missingList_throws() {
        when(routeListRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listRouteService.delete(1L, 10L, self(1L)))
                .isInstanceOf(RouteListNotFoundException.class);

        verify(listRouteRepository, never()).deleteById(any());
    }

    @Test
    void delete_byAnotherRegularUser_throwsForbidden() {
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(routeList(1L)));

        assertThatThrownBy(() -> listRouteService.delete(1L, 10L, self(2L))).isInstanceOf(ForbiddenException.class);

        verify(listRouteRepository, never()).deleteById(any());
    }

    @Test
    void delete_byAdmin_deletesFromAnotherUsersList() {
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(routeList(1L)));
        when(listRouteRepository.existsById(new ListRouteId(1L, 10L))).thenReturn(true);

        listRouteService.delete(1L, 10L, admin(99L));

        verify(listRouteRepository).deleteById(new ListRouteId(1L, 10L));
    }
}
