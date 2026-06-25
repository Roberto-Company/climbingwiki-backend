package com.roberto.climbingwiki.project1.repositories;

import com.roberto.climbingwiki.project1.model.entities.ListRoute;
import com.roberto.climbingwiki.project1.model.entities.ListRouteId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListRouteRepository extends JpaRepository<ListRoute, ListRouteId> {

    List<ListRoute> findByListId(Long listId);

    List<ListRoute> findByRouteId(Long routeId);
}
