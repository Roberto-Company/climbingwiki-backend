package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByRouteId(Long routeId, Pageable pageable);

    Page<Comment> findByCragId(Long cragId, Pageable pageable);
}
