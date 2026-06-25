package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.Grade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByGradingSystemId(Long gradingSystemId);

    Optional<Grade> findByGradingSystemIdAndLabel(Long gradingSystemId, String label);
}
