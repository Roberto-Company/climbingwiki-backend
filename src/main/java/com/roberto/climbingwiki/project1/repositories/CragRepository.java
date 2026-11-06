package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.Crag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CragRepository extends JpaRepository<Crag, Long>, JpaSpecificationExecutor<Crag> {

    Optional<Crag> findByExternalSourceAndExternalId(String externalSource, String externalId);
}
