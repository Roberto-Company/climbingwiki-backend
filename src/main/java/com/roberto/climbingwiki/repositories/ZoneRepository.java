package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.Zone;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ZoneRepository extends JpaRepository<Zone, Long>, JpaSpecificationExecutor<Zone> {

    List<Zone> findByParentZoneId(Long parentZoneId);

    Optional<Zone> findByExternalSourceAndExternalId(String externalSource, String externalId);

    boolean existsByParentZoneId(Long parentZoneId);
}
