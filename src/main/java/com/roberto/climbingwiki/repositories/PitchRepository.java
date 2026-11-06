package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.Pitch;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PitchRepository extends JpaRepository<Pitch, Long> {

    Page<Pitch> findByRouteIdOrderByOrderIndexAsc(Long routeId, Pageable pageable);

    Optional<Pitch> findByRouteIdAndOrderIndex(Long routeId, Integer orderIndex);

    /** Every pitch of the given routes, in one query, for building route summaries. */
    @Query("""
            select new com.roberto.climbingwiki.repositories.PitchSummaryRow(
                p.route.id, g.id, g.difficultyScore, p.lengthM)
            from Pitch p left join p.grade g
            where p.route.id in :routeIds
            """)
    List<PitchSummaryRow> findSummaryRows(@Param("routeIds") Collection<Long> routeIds);

    boolean existsByRouteId(Long routeId);

    /** Newest unverified items for the moderation queue, with creator and parent loaded in the same query. */
    @EntityGraph(attributePaths = {"createdBy", "route"})
    List<Pitch> findTop100ByVerifiedFalseOrderByIdDesc();
}
