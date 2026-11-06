package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.Crag;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CragRepository extends JpaRepository<Crag, Long>, JpaSpecificationExecutor<Crag> {

    Optional<Crag> findByExternalSourceAndExternalId(String externalSource, String externalId);

    boolean existsByZoneId(Long zoneId);

    /** Newest unverified items for the moderation queue, with creator and parent loaded in the same query. */
    @EntityGraph(attributePaths = {"createdBy", "zone"})
    List<Crag> findTop100ByVerifiedFalseOrderByIdDesc();
}
