package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.Crag;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CragRepository extends JpaRepository<Crag, Long> {

    Page<Crag> findByZoneId(Long zoneId, Pageable pageable);

    Optional<Crag> findByExternalSourceAndExternalId(String externalSource, String externalId);
}
