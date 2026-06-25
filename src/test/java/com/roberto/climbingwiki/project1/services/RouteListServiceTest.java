package com.roberto.climbingwiki.project1.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.project1.dtos.RouteListRequest;
import com.roberto.climbingwiki.project1.dtos.RouteListResponse;
import com.roberto.climbingwiki.project1.exceptions.ForbiddenException;
import com.roberto.climbingwiki.project1.exceptions.RouteListNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.RouteList;
import com.roberto.climbingwiki.project1.model.entities.User;
import com.roberto.climbingwiki.project1.repositories.RouteListRepository;
import com.roberto.climbingwiki.project1.repositories.UserRepository;
import com.roberto.climbingwiki.project1.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteListServiceTest {

    @Mock
    private RouteListRepository routeListRepository;

    @Mock
    private UserRepository userRepository;

    private RouteListService routeListService;

    @BeforeEach
    void setUp() {
        routeListService = new RouteListService(routeListRepository, userRepository);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private RouteList routeList(Long id, String name) {
        RouteList routeList = new RouteList();
        routeList.setId(id);
        routeList.setUser(user(1L));
        routeList.setName(name);
        return routeList;
    }

    private RouteListRequest request(Boolean isPublic) {
        return new RouteListRequest(1L, "Classics", "desc", isPublic);
    }

    private AuthenticatedUser self(Long id) {
        return new AuthenticatedUser(id, "jane", User.Role.USER);
    }

    private AuthenticatedUser admin(Long id) {
        return new AuthenticatedUser(id, "boss", User.Role.ADMIN);
    }

    @Test
    void findAll_returnsMappedResponses() {
        when(routeListRepository.findAll())
                .thenReturn(List.of(routeList(1L, "Classics"), routeList(2L, "Projects")));

        List<RouteListResponse> result = routeListService.findAll();

        assertThat(result).extracting(RouteListResponse::name).containsExactly("Classics", "Projects");
    }

    @Test
    void findByUser_returnsListsOwnedByThatUser() {
        when(routeListRepository.findByUserId(1L)).thenReturn(List.of(routeList(1L, "Classics")));

        List<RouteListResponse> result = routeListService.findByUser(1L);

        assertThat(result).extracting(RouteListResponse::userId).containsExactly(1L);
    }

    @Test
    void findById_existingList_returnsResponse() {
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(routeList(1L, "Classics")));

        RouteListResponse result = routeListService.findById(1L);

        assertThat(result.name()).isEqualTo("Classics");
    }

    @Test
    void findById_missingList_throws() {
        when(routeListRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeListService.findById(99L))
                .isInstanceOf(RouteListNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_missingUser_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeListService.create(request(true), self(1L)))
                .isInstanceOf(UserNotFoundException.class);

        verify(routeListRepository, never()).save(any());
    }

    @Test
    void create_withExplicitIsPublicFalse_savesAsPrivate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeListRepository.save(any(RouteList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteListResponse result = routeListService.create(request(false), self(1L));

        assertThat(result.isPublic()).isFalse();
    }

    @Test
    void create_withNullIsPublic_defaultsToTrue() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeListRepository.save(any(RouteList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteListResponse result = routeListService.create(request(null), self(1L));

        assertThat(result.isPublic()).isTrue();
    }

    @Test
    void create_byAnotherRegularUser_throwsForbidden() {
        assertThatThrownBy(() -> routeListService.create(request(true), self(2L)))
                .isInstanceOf(ForbiddenException.class);

        verify(routeListRepository, never()).save(any());
    }

    @Test
    void update_existingList_appliesChanges() {
        RouteList existing = routeList(1L, "Old name");
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeListRepository.save(any(RouteList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteListResponse result = routeListService.update(1L, request(true), self(1L));

        assertThat(result.name()).isEqualTo("Classics");
    }

    @Test
    void update_missingList_throws() {
        when(routeListRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeListService.update(99L, request(true), self(1L)))
                .isInstanceOf(RouteListNotFoundException.class);
    }

    @Test
    void update_byAnotherRegularUser_throwsForbidden() {
        RouteList existing = routeList(1L, "Old name");
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> routeListService.update(1L, request(true), self(2L)))
                .isInstanceOf(ForbiddenException.class);

        verify(routeListRepository, never()).save(any());
    }

    @Test
    void update_byModerator_succeeds() {
        RouteList existing = routeList(1L, "Old name");
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeListRepository.save(any(RouteList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteListResponse result = routeListService.update(
                1L, request(true), new AuthenticatedUser(99L, "mod", User.Role.MODERATOR));

        assertThat(result.name()).isEqualTo("Classics");
    }

    @Test
    void delete_existingList_deletes() {
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(routeList(1L, "Classics")));

        routeListService.delete(1L, self(1L));

        verify(routeListRepository).deleteById(1L);
    }

    @Test
    void delete_missingList_throwsAndDoesNotDelete() {
        when(routeListRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeListService.delete(99L, self(1L)))
                .isInstanceOf(RouteListNotFoundException.class);

        verify(routeListRepository, never()).deleteById(99L);
    }

    @Test
    void delete_byAnotherRegularUser_throwsForbiddenAndDoesNotDelete() {
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(routeList(1L, "Classics")));

        assertThatThrownBy(() -> routeListService.delete(1L, self(2L))).isInstanceOf(ForbiddenException.class);

        verify(routeListRepository, never()).deleteById(any());
    }

    @Test
    void delete_byAdmin_deletesAnotherUsersList() {
        when(routeListRepository.findById(1L)).thenReturn(Optional.of(routeList(1L, "Classics")));

        routeListService.delete(1L, admin(99L));

        verify(routeListRepository).deleteById(1L);
    }
}
