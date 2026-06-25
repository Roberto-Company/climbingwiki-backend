package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.Rating;
import com.roberto.climbingwiki.project1.model.entities.RatingId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, RatingId> {

    List<Rating> findByRouteId(Long routeId);

    List<Rating> findByUserId(Long userId);
}
