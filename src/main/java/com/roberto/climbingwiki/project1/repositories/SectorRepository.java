package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.Sector;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    Page<Sector> findByCragId(Long cragId, Pageable pageable);

    Optional<Sector> findByExternalSourceAndExternalId(String externalSource, String externalId);
}
