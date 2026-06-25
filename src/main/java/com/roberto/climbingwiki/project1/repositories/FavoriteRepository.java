package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.Favorite;
import com.roberto.climbingwiki.project1.model.entities.FavoriteId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    List<Favorite> findByRouteId(Long routeId);

    List<Favorite> findByUserId(Long userId);
}
