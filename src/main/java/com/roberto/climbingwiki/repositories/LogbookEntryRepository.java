package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.LogbookEntry;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogbookEntryRepository extends JpaRepository<LogbookEntry, Long> {

    Page<LogbookEntry> findByUserId(Long userId, Pageable pageable);

    Page<LogbookEntry> findByRouteId(Long routeId, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndResultIn(Long userId, Collection<LogbookEntry.Result> results);

    /**
     * Grade ids of the pitches on routes the user logged with one of {@code results}, hardest first.
     * Ask for a single-row page to get the hardest one.
     */
    @Query("""
            select g.id from LogbookEntry e
            join Pitch p on p.route = e.route
            join p.grade g
            where e.user.id = :userId and e.result in :results
            order by g.difficultyScore desc, g.id
            """)
    List<Long> findGradeIdsByDifficulty(
            @Param("userId") Long userId,
            @Param("results") Collection<LogbookEntry.Result> results,
            Pageable pageable);
}
