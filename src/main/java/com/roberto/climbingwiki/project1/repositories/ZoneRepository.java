package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.Zone;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    List<Zone> findByParentZoneId(Long parentZoneId);

    Page<Zone> findByParentZoneId(Long parentZoneId, Pageable pageable);

    Page<Zone> findByParentZoneIsNull(Pageable pageable);

    Optional<Zone> findByExternalSourceAndExternalId(String externalSource, String externalId);
}
