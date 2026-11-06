package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.dtos.ListRouteRequest;
import com.roberto.climbingwiki.dtos.ListRouteResponse;
import com.roberto.climbingwiki.dtos.UpsertResult;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.ListRouteNotFoundException;
import com.roberto.climbingwiki.exceptions.RouteListNotFoundException;
import com.roberto.climbingwiki.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.model.entities.ListRoute;
import com.roberto.climbingwiki.model.entities.ListRouteId;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.RouteList;
import com.roberto.climbingwiki.repositories.ListRouteRepository;
import com.roberto.climbingwiki.repositories.RouteListRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListRouteService {

    private final ListRouteRepository listRouteRepository;
    private final RouteListRepository routeListRepository;
    private final RouteRepository routeRepository;

    public ListRouteService(
            ListRouteRepository listRouteRepository,
            RouteListRepository routeListRepository,
            RouteRepository routeRepository) {
        this.listRouteRepository = listRouteRepository;
        this.routeListRepository = routeListRepository;
        this.routeRepository = routeRepository;
    }

    public List<ListRouteResponse> findAll() {
        return listRouteRepository.findAll().stream().map(ListRouteResponse::from).toList();
    }

    public List<ListRouteResponse> findByList(Long listId) {
        return listRouteRepository.findByListId(listId).stream().map(ListRouteResponse::from).toList();
    }

    public List<ListRouteResponse> findByRoute(Long routeId) {
        return listRouteRepository.findByRouteId(routeId).stream().map(ListRouteResponse::from).toList();
    }

    public ListRouteResponse findOne(Long listId, Long routeId) {
        return listRouteRepository
                .findById(new ListRouteId(listId, routeId))
                .map(ListRouteResponse::from)
                .orElseThrow(() -> new ListRouteNotFoundException(listId, routeId));
    }

    @Transactional
    public UpsertResult<ListRouteResponse> upsert(ListRouteRequest request, AuthenticatedUser requester) {
        RouteList routeList = routeListRepository
                .findById(request.listId())
                .orElseThrow(() -> new RouteListNotFoundException(request.listId()));
        if (!requester.isSelfOrModeratorOrAdmin(routeList.getUser().getId())) {
            throw new ForbiddenException();
        }

        ListRouteId id = new ListRouteId(request.listId(), request.routeId());
        boolean created = !listRouteRepository.existsById(id);

        ListRoute listRoute = listRouteRepository.findById(id).orElseGet(ListRoute::new);

        Route route = routeRepository
                .findById(request.routeId())
                .orElseThrow(() -> new RouteNotFoundException(request.routeId()));

        listRoute.setList(routeList);
        listRoute.setRoute(route);

        ListRoute saved = listRouteRepository.save(listRoute);
        return new UpsertResult<>(ListRouteResponse.from(saved), created);
    }

    @Transactional
    public void delete(Long listId, Long routeId, AuthenticatedUser requester) {
        RouteList routeList =
                routeListRepository.findById(listId).orElseThrow(() -> new RouteListNotFoundException(listId));
        if (!requester.isSelfOrModeratorOrAdmin(routeList.getUser().getId())) {
            throw new ForbiddenException();
        }
        ListRouteId id = new ListRouteId(listId, routeId);
        if (listRouteRepository.existsById(id)) {
            listRouteRepository.deleteById(id);
        }
    }
}
