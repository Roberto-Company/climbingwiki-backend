package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    Page<Photo> findByRouteId(Long routeId, Pageable pageable);

    Page<Photo> findByCragId(Long cragId, Pageable pageable);
}
