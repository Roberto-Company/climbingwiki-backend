package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.Pitch;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PitchRepository extends JpaRepository<Pitch, Long> {

    Page<Pitch> findByRouteIdOrderByOrderIndexAsc(Long routeId, Pageable pageable);

    Optional<Pitch> findByRouteIdAndOrderIndex(Long routeId, Integer orderIndex);
}
