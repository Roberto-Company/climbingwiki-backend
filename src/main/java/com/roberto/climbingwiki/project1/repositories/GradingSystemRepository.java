package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.GradingSystem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradingSystemRepository extends JpaRepository<GradingSystem, Long> {

    Optional<GradingSystem> findByName(String name);
}
