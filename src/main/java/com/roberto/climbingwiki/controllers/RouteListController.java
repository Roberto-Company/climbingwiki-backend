package com.roberto.climbingwiki.controllers;

import com.roberto.climbingwiki.dtos.RouteListRequest;
import com.roberto.climbingwiki.dtos.RouteListResponse;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import com.roberto.climbingwiki.services.RouteListService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/route-lists")
public class RouteListController {

    private final RouteListService routeListService;

    public RouteListController(RouteListService routeListService) {
        this.routeListService = routeListService;
    }

    @GetMapping
    public List<RouteListResponse> list(@RequestParam(required = false) Long userId) {
        if (userId != null) {
            return routeListService.findByUser(userId);
        }
        return routeListService.findAll();
    }

    @GetMapping("/{id}")
    public RouteListResponse get(@PathVariable Long id) {
        return routeListService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteListResponse create(
            @Valid @RequestBody RouteListRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        return routeListService.create(request, principal);
    }

    @PutMapping("/{id}")
    public RouteListResponse update(
            @PathVariable Long id,
            @Valid @RequestBody RouteListRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return routeListService.update(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        routeListService.delete(id, principal);
    }
}
