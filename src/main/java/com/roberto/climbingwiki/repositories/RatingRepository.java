package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.Rating;
import com.roberto.climbingwiki.model.entities.RatingId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, RatingId> {

    List<Rating> findByRouteId(Long routeId);

    List<Rating> findByUserId(Long userId);
}
