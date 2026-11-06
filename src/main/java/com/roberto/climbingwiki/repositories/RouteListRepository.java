package com.roberto.climbingwiki.repositories;

import com.roberto.climbingwiki.model.entities.RouteList;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteListRepository extends JpaRepository<RouteList, Long> {

    List<RouteList> findByUserId(Long userId);
}
