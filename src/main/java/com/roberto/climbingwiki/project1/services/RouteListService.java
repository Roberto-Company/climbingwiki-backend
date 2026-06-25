package com.roberto.climbingwiki.project1.services;

import com.roberto.climbingwiki.project1.dtos.RouteListRequest;
import com.roberto.climbingwiki.project1.dtos.RouteListResponse;
import com.roberto.climbingwiki.project1.exceptions.ForbiddenException;
import com.roberto.climbingwiki.project1.exceptions.RouteListNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.RouteList;
import com.roberto.climbingwiki.project1.model.entities.User;
import com.roberto.climbingwiki.project1.repositories.RouteListRepository;
import com.roberto.climbingwiki.project1.repositories.UserRepository;
import com.roberto.climbingwiki.project1.security.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteListService {

    private final RouteListRepository routeListRepository;
    private final UserRepository userRepository;

    public RouteListService(RouteListRepository routeListRepository, UserRepository userRepository) {
        this.routeListRepository = routeListRepository;
        this.userRepository = userRepository;
    }

    public List<RouteListResponse> findAll() {
        return routeListRepository.findAll().stream().map(RouteListResponse::from).toList();
    }

    public List<RouteListResponse> findByUser(Long userId) {
        return routeListRepository.findByUserId(userId).stream().map(RouteListResponse::from).toList();
    }

    public RouteListResponse findById(Long id) {
        return routeListRepository
                .findById(id)
                .map(RouteListResponse::from)
                .orElseThrow(() -> new RouteListNotFoundException(id));
    }

    @Transactional
    public RouteListResponse create(RouteListRequest request, AuthenticatedUser requester) {
        if (!requester.isSelfOrModeratorOrAdmin(request.userId())) {
            throw new ForbiddenException();
        }
        RouteList routeList = new RouteList();
        applyRequest(routeList, request);
        return RouteListResponse.from(routeListRepository.save(routeList));
    }

    @Transactional
    public RouteListResponse update(Long id, RouteListRequest request, AuthenticatedUser requester) {
        RouteList routeList =
                routeListRepository.findById(id).orElseThrow(() -> new RouteListNotFoundException(id));
        if (!requester.isSelfOrModeratorOrAdmin(routeList.getUser().getId())) {
            throw new ForbiddenException();
        }
        applyRequest(routeList, request);
        return RouteListResponse.from(routeListRepository.save(routeList));
    }

    @Transactional
    public void delete(Long id, AuthenticatedUser requester) {
        RouteList routeList =
                routeListRepository.findById(id).orElseThrow(() -> new RouteListNotFoundException(id));
        if (!requester.isSelfOrModeratorOrAdmin(routeList.getUser().getId())) {
            throw new ForbiddenException();
        }
        routeListRepository.deleteById(id);
    }

    private void applyRequest(RouteList routeList, RouteListRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        routeList.setUser(user);
        routeList.setName(request.name());
        routeList.setDescription(request.description());
        routeList.setPublic(request.isPublic() == null || request.isPublic());
    }
}
