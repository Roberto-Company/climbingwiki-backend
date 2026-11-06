package com.roberto.climbingwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.dtos.RatingRequest;
import com.roberto.climbingwiki.dtos.RatingResponse;
import com.roberto.climbingwiki.dtos.UpsertResult;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.InvalidRatingScoreException;
import com.roberto.climbingwiki.exceptions.RatingNotFoundException;
import com.roberto.climbingwiki.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.model.entities.Rating;
import com.roberto.climbingwiki.model.entities.RatingId;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.RatingRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.repositories.UserRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RouteRepository routeRepository;

    private RatingService ratingService;

    @BeforeEach
    void setUp() {
        ratingService = new RatingService(ratingRepository, userRepository, routeRepository);
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

    private Rating rating(Long userId, Long routeId, BigDecimal score) {
        Rating rating = new Rating();
        rating.setUser(user(userId));
        rating.setRoute(route(routeId));
        rating.setScore(score);
        return rating;
    }

    private RatingRequest request(Long userId, Long routeId, BigDecimal score) {
        return new RatingRequest(userId, routeId, score);
    }

    private AuthenticatedUser self(Long id) {
        return new AuthenticatedUser(id, "jane", User.Role.USER);
    }

    private AuthenticatedUser admin(Long id) {
        return new AuthenticatedUser(id, "boss", User.Role.ADMIN);
    }

    @Test
    void findAll_returnsMappedResponses() {
        when(ratingRepository.findAll())
                .thenReturn(List.of(rating(1L, 1L, BigDecimal.valueOf(4.5)), rating(2L, 1L, BigDecimal.valueOf(3.0))));

        List<RatingResponse> result = ratingService.findAll();

        assertThat(result).extracting(RatingResponse::score)
                .containsExactly(BigDecimal.valueOf(4.5), BigDecimal.valueOf(3.0));
    }

    @Test
    void findByRoute_returnsRatingsForThatRoute() {
        when(ratingRepository.findByRouteId(1L)).thenReturn(List.of(rating(1L, 1L, BigDecimal.valueOf(4.5))));

        List<RatingResponse> result = ratingService.findByRoute(1L);

        assertThat(result).extracting(RatingResponse::routeId).containsExactly(1L);
    }

    @Test
    void findByUser_returnsRatingsByThatUser() {
        when(ratingRepository.findByUserId(1L)).thenReturn(List.of(rating(1L, 7L, BigDecimal.valueOf(2.0))));

        List<RatingResponse> result = ratingService.findByUser(1L);

        assertThat(result).extracting(RatingResponse::userId).containsExactly(1L);
    }

    @Test
    void findOne_existingRating_returnsResponse() {
        when(ratingRepository.findById(new RatingId(1L, 1L)))
                .thenReturn(Optional.of(rating(1L, 1L, BigDecimal.valueOf(4.5))));

        RatingResponse result = ratingService.findOne(1L, 1L);

        assertThat(result.score()).isEqualTo(BigDecimal.valueOf(4.5));
    }

    @Test
    void findOne_missingRating_throws() {
        when(ratingRepository.findById(new RatingId(1L, 99L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.findOne(1L, 99L))
                .isInstanceOf(RatingNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void upsert_invalidScoreStep_throws() {
        assertThatThrownBy(() -> ratingService.upsert(request(1L, 1L, BigDecimal.valueOf(3.3)), self(1L)))
                .isInstanceOf(InvalidRatingScoreException.class);

        verify(ratingRepository, never()).save(any());
    }

    @Test
    void upsert_missingUser_throws() {
        when(ratingRepository.existsById(any())).thenReturn(false);
        when(ratingRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.upsert(request(1L, 1L, BigDecimal.valueOf(4.0)), self(1L)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void upsert_missingRoute_throws() {
        when(ratingRepository.existsById(any())).thenReturn(false);
        when(ratingRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.upsert(request(1L, 1L, BigDecimal.valueOf(4.0)), self(1L)))
                .isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void upsert_newRating_createsAndReturnsCreatedTrue() {
        when(ratingRepository.existsById(new RatingId(1L, 1L))).thenReturn(false);
        when(ratingRepository.findById(new RatingId(1L, 1L))).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpsertResult<RatingResponse> result =
                ratingService.upsert(request(1L, 1L, BigDecimal.valueOf(4.5)), self(1L));

        assertThat(result.created()).isTrue();
        assertThat(result.data().score()).isEqualTo(BigDecimal.valueOf(4.5));
    }

    @Test
    void upsert_existingRating_updatesAndReturnsCreatedFalse() {
        Rating existing = rating(1L, 1L, BigDecimal.valueOf(3.0));
        when(ratingRepository.existsById(new RatingId(1L, 1L))).thenReturn(true);
        when(ratingRepository.findById(new RatingId(1L, 1L))).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpsertResult<RatingResponse> result =
                ratingService.upsert(request(1L, 1L, BigDecimal.valueOf(5.0)), self(1L));

        assertThat(result.created()).isFalse();
        assertThat(result.data().score()).isEqualTo(BigDecimal.valueOf(5.0));
    }

    @Test
    void upsert_byAnotherRegularUser_throwsForbidden() {
        assertThatThrownBy(() -> ratingService.upsert(request(1L, 1L, BigDecimal.valueOf(4.5)), self(2L)))
                .isInstanceOf(ForbiddenException.class);

        verify(ratingRepository, never()).save(any());
    }

    @Test
    void upsert_byModerator_succeeds() {
        when(ratingRepository.existsById(new RatingId(1L, 1L))).thenReturn(false);
        when(ratingRepository.findById(new RatingId(1L, 1L))).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpsertResult<RatingResponse> result = ratingService.upsert(
                request(1L, 1L, BigDecimal.valueOf(4.5)), new AuthenticatedUser(99L, "mod", User.Role.MODERATOR));

        assertThat(result.data().score()).isEqualTo(BigDecimal.valueOf(4.5));
    }

    @Test
    void delete_existingRating_deletes() {
        when(ratingRepository.existsById(new RatingId(1L, 1L))).thenReturn(true);

        ratingService.delete(1L, 1L, self(1L));

        verify(ratingRepository).deleteById(new RatingId(1L, 1L));
    }

    @Test
    void delete_missingRating_isNoOpAndDoesNotThrow() {
        when(ratingRepository.existsById(new RatingId(1L, 99L))).thenReturn(false);

        ratingService.delete(1L, 99L, self(1L));

        verify(ratingRepository, never()).deleteById(any());
    }

    @Test
    void delete_byAnotherRegularUser_throwsForbidden() {
        assertThatThrownBy(() -> ratingService.delete(1L, 1L, self(2L))).isInstanceOf(ForbiddenException.class);

        verify(ratingRepository, never()).deleteById(any());
    }

    @Test
    void delete_byAdmin_deletesAnotherUsersRating() {
        when(ratingRepository.existsById(new RatingId(1L, 1L))).thenReturn(true);

        ratingService.delete(1L, 1L, admin(99L));

        verify(ratingRepository).deleteById(new RatingId(1L, 1L));
    }
}
