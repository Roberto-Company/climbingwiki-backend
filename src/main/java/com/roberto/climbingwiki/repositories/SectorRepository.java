package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.Sector;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    Page<Sector> findByCragId(Long cragId, Pageable pageable);

    Optional<Sector> findByExternalSourceAndExternalId(String externalSource, String externalId);

    boolean existsByCragId(Long cragId);

    /** Newest unverified items for the moderation queue, with creator and parent loaded in the same query. */
    @EntityGraph(attributePaths = {"createdBy", "crag"})
    List<Sector> findTop100ByVerifiedFalseOrderByIdDesc();
}
