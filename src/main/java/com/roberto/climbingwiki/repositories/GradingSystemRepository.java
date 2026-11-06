package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.GradingSystem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradingSystemRepository extends JpaRepository<GradingSystem, Long> {

    Optional<GradingSystem> findByName(String name);
}
