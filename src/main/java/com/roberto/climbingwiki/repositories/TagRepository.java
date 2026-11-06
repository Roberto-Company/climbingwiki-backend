package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {}
