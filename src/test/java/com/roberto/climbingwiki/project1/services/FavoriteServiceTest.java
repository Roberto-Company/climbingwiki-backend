package com.roberto.climbingwiki.project1.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.project1.dtos.FavoriteRequest;
import com.roberto.climbingwiki.project1.dtos.FavoriteResponse;
import com.roberto.climbingwiki.project1.dtos.UpsertResult;
import com.roberto.climbingwiki.project1.exceptions.FavoriteNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.ForbiddenException;
import com.roberto.climbingwiki.project1.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.Favorite;
import com.roberto.climbingwiki.project1.model.entities.FavoriteId;
import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.model.entities.User;
import com.roberto.climbingwiki.project1.repositories.FavoriteRepository;
import com.roberto.climbingwiki.project1.repositories.RouteRepository;
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
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RouteRepository routeRepository;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteRepository, userRepository, routeRepository);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Route route(Long id) {
        Route route = new Route();
        route.setId(id);
        return route;
    }

    private Favorite favorite(Long userId, Long routeId) {
        Favorite favorite = new Favorite();
        favorite.setUser(user(userId));
        favorite.setRoute(route(routeId));
        return favorite;
    }

    private FavoriteRequest request(Long userId, Long routeId) {
        return new FavoriteRequest(userId, routeId);
    }

    private AuthenticatedUser self(Long id) {
        return new AuthenticatedUser(id, "jane", User.Role.USER);
    }

    private AuthenticatedUser admin(Long id) {
        return new AuthenticatedUser(id, "boss", User.Role.ADMIN);
    }

    @Test
    void findAll_returnsMappedResponses() {
        when(favoriteRepository.findAll()).thenReturn(List.of(favorite(1L, 1L), favorite(2L, 1L)));

        List<FavoriteResponse> result = favoriteService.findAll();

        assertThat(result).extracting(FavoriteResponse::userId).containsExactly(1L, 2L);
    }

    @Test
    void findByRoute_returnsFavoritesForThatRoute() {
        when(favoriteRepository.findByRouteId(1L)).thenReturn(List.of(favorite(1L, 1L)));

        List<FavoriteResponse> result = favoriteService.findByRoute(1L);

        assertThat(result).extracting(FavoriteResponse::routeId).containsExactly(1L);
    }

    @Test
    void findByUser_returnsFavoritesByThatUser() {
        when(favoriteRepository.findByUserId(1L)).thenReturn(List.of(favorite(1L, 7L)));

        List<FavoriteResponse> result = favoriteService.findByUser(1L);

        assertThat(result).extracting(FavoriteResponse::userId).containsExactly(1L);
    }

    @Test
    void findOne_existingFavorite_returnsResponse() {
        when(favoriteRepository.findById(new FavoriteId(1L, 1L))).thenReturn(Optional.of(favorite(1L, 1L)));

        FavoriteResponse result = favoriteService.findOne(1L, 1L);

        assertThat(result.routeId()).isEqualTo(1L);
    }

    @Test
    void findOne_missingFavorite_throws() {
        when(favoriteRepository.findById(new FavoriteId(1L, 99L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.findOne(1L, 99L))
                .isInstanceOf(FavoriteNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void upsert_missingUser_throws() {
        when(favoriteRepository.existsById(any())).thenReturn(false);
        when(favoriteRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.upsert(request(1L, 1L), self(1L)))
                .isInstanceOf(UserNotFoundException.class);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void upsert_missingRoute_throws() {
        when(favoriteRepository.existsById(any())).thenReturn(false);
        when(favoriteRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.upsert(request(1L, 1L), self(1L)))
                .isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void upsert_newFavorite_createsAndReturnsCreatedTrue() {
        when(favoriteRepository.existsById(new FavoriteId(1L, 1L))).thenReturn(false);
        when(favoriteRepository.findById(new FavoriteId(1L, 1L))).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpsertResult<FavoriteResponse> result = favoriteService.upsert(request(1L, 1L), self(1L));

        assertThat(result.created()).isTrue();
        assertThat(result.data().routeId()).isEqualTo(1L);
    }

    @Test
    void upsert_existingFavorite_isNoOpAndReturnsCreatedFalse() {
        Favorite existing = favorite(1L, 1L);
        when(favoriteRepository.existsById(new FavoriteId(1L, 1L))).thenReturn(true);
        when(favoriteRepository.findById(new FavoriteId(1L, 1L))).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpsertResult<FavoriteResponse> result = favoriteService.upsert(request(1L, 1L), self(1L));

        assertThat(result.created()).isFalse();
    }

    @Test
    void upsert_byAnotherRegularUser_throwsForbidden() {
        assertThatThrownBy(() -> favoriteService.upsert(request(1L, 1L), self(2L)))
                .isInstanceOf(ForbiddenException.class);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void upsert_byModerator_succeeds() {
        when(favoriteRepository.existsById(new FavoriteId(1L, 1L))).thenReturn(false);
        when(favoriteRepository.findById(new FavoriteId(1L, 1L))).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpsertResult<FavoriteResponse> result = favoriteService.upsert(
                request(1L, 1L), new AuthenticatedUser(99L, "mod", User.Role.MODERATOR));

        assertThat(result.data().routeId()).isEqualTo(1L);
    }

    @Test
    void delete_existingFavorite_deletes() {
        when(favoriteRepository.existsById(new FavoriteId(1L, 1L))).thenReturn(true);

        favoriteService.delete(1L, 1L, self(1L));

        verify(favoriteRepository).deleteById(new FavoriteId(1L, 1L));
    }

    @Test
    void delete_missingFavorite_isNoOpAndDoesNotThrow() {
        when(favoriteRepository.existsById(new FavoriteId(1L, 99L))).thenReturn(false);

        favoriteService.delete(1L, 99L, self(1L));

        verify(favoriteRepository, never()).deleteById(any());
    }

    @Test
    void delete_byAnotherRegularUser_throwsForbidden() {
        assertThatThrownBy(() -> favoriteService.delete(1L, 1L, self(2L))).isInstanceOf(ForbiddenException.class);

        verify(favoriteRepository, never()).deleteById(any());
    }

    @Test
    void delete_byAdmin_deletesAnotherUsersFavorite() {
        when(favoriteRepository.existsById(new FavoriteId(1L, 1L))).thenReturn(true);

        favoriteService.delete(1L, 1L, admin(99L));

        verify(favoriteRepository).deleteById(new FavoriteId(1L, 1L));
    }
}
