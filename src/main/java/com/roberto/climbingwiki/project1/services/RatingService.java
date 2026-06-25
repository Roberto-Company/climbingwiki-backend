package com.roberto.climbingwiki.project1.services;

import com.roberto.climbingwiki.project1.dtos.RatingRequest;
import com.roberto.climbingwiki.project1.dtos.RatingResponse;
import com.roberto.climbingwiki.project1.dtos.UpsertResult;
import com.roberto.climbingwiki.project1.exceptions.ForbiddenException;
import com.roberto.climbingwiki.project1.exceptions.InvalidRatingScoreException;
import com.roberto.climbingwiki.project1.exceptions.RatingNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.Rating;
import com.roberto.climbingwiki.project1.model.entities.RatingId;
import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.model.entities.User;
import com.roberto.climbingwiki.project1.repositories.RatingRepository;
import com.roberto.climbingwiki.project1.repositories.RouteRepository;
import com.roberto.climbingwiki.project1.repositories.UserRepository;
import com.roberto.climbingwiki.project1.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;

    public RatingService(
            RatingRepository ratingRepository, UserRepository userRepository, RouteRepository routeRepository) {
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
    }

    public List<RatingResponse> findAll() {
        return ratingRepository.findAll().stream().map(RatingResponse::from).toList();
    }

    public List<RatingResponse> findByRoute(Long routeId) {
        return ratingRepository.findByRouteId(routeId).stream().map(RatingResponse::from).toList();
    }

    public List<RatingResponse> findByUser(Long userId) {
        return ratingRepository.findByUserId(userId).stream().map(RatingResponse::from).toList();
    }

    public RatingResponse findOne(Long userId, Long routeId) {
        return ratingRepository.findById(new RatingId(userId, routeId))
                .map(RatingResponse::from)
                .orElseThrow(() -> new RatingNotFoundException(userId, routeId));
    }

    @Transactional
    public UpsertResult<RatingResponse> upsert(RatingRequest request, AuthenticatedUser requester) {
        if (!requester.isSelfOrModeratorOrAdmin(request.userId())) {
            throw new ForbiddenException();
        }
        validateScoreStep(request.score());

        RatingId id = new RatingId(request.userId(), request.routeId());
        boolean created = !ratingRepository.existsById(id);

        Rating rating = ratingRepository.findById(id).orElseGet(Rating::new);

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new RouteNotFoundException(request.routeId()));

        rating.setUser(user);
        rating.setRoute(route);
        rating.setScore(request.score());

        Rating saved = ratingRepository.save(rating);
        return new UpsertResult<>(RatingResponse.from(saved), created);
    }

    @Transactional
    public void delete(Long userId, Long routeId, AuthenticatedUser requester) {
        if (!requester.isSelfOrModeratorOrAdmin(userId)) {
            throw new ForbiddenException();
        }
        RatingId id = new RatingId(userId, routeId);
        if (ratingRepository.existsById(id)) {
            ratingRepository.deleteById(id);
        }
    }

    private void validateScoreStep(BigDecimal score) {
        BigDecimal doubled = score.multiply(BigDecimal.valueOf(2));
        if (doubled.stripTrailingZeros().scale() > 0) {
            throw new InvalidRatingScoreException(score);
        }
    }
}
