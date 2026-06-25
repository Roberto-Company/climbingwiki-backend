package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.LogbookEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogbookEntryRepository extends JpaRepository<LogbookEntry, Long> {

    Page<LogbookEntry> findByUserId(Long userId, Pageable pageable);

    Page<LogbookEntry> findByRouteId(Long routeId, Pageable pageable);
}
