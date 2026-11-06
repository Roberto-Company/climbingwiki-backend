package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.Favorite;
import com.roberto.climbingwiki.model.entities.FavoriteId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    List<Favorite> findByRouteId(Long routeId);

    List<Favorite> findByUserId(Long userId);
}
