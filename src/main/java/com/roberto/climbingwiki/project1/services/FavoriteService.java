package com.roberto.climbingwiki.project1.services;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;

    public FavoriteService(
            FavoriteRepository favoriteRepository, UserRepository userRepository, RouteRepository routeRepository) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
    }

    public List<FavoriteResponse> findAll() {
        return favoriteRepository.findAll().stream().map(FavoriteResponse::from).toList();
    }

    public List<FavoriteResponse> findByRoute(Long routeId) {
        return favoriteRepository.findByRouteId(routeId).stream().map(FavoriteResponse::from).toList();
    }

    public List<FavoriteResponse> findByUser(Long userId) {
        return favoriteRepository.findByUserId(userId).stream().map(FavoriteResponse::from).toList();
    }

    public FavoriteResponse findOne(Long userId, Long routeId) {
        return favoriteRepository.findById(new FavoriteId(userId, routeId))
                .map(FavoriteResponse::from)
                .orElseThrow(() -> new FavoriteNotFoundException(userId, routeId));
    }

    @Transactional
    public UpsertResult<FavoriteResponse> upsert(FavoriteRequest request, AuthenticatedUser requester) {
        if (!requester.isSelfOrModeratorOrAdmin(request.userId())) {
            throw new ForbiddenException();
        }
        FavoriteId id = new FavoriteId(request.userId(), request.routeId());
        boolean created = !favoriteRepository.existsById(id);

        Favorite favorite = favoriteRepository.findById(id).orElseGet(Favorite::new);

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new RouteNotFoundException(request.routeId()));

        favorite.setUser(user);
        favorite.setRoute(route);

        Favorite saved = favoriteRepository.save(favorite);
        return new UpsertResult<>(FavoriteResponse.from(saved), created);
    }

    @Transactional
    public void delete(Long userId, Long routeId, AuthenticatedUser requester) {
        if (!requester.isSelfOrModeratorOrAdmin(userId)) {
            throw new ForbiddenException();
        }
        FavoriteId id = new FavoriteId(userId, routeId);
        if (favoriteRepository.existsById(id)) {
            favoriteRepository.deleteById(id);
        }
    }
}
