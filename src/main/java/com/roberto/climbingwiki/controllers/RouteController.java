package com.roberto.climbingwiki.controllers;

import com.roberto.climbingwiki.dtos.PagedResponse;
import com.roberto.climbingwiki.dtos.RouteRequest;
import com.roberto.climbingwiki.dtos.RouteResponse;
import com.roberto.climbingwiki.dtos.VerificationRequest;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import com.roberto.climbingwiki.services.RouteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public PagedResponse<RouteResponse> list(
            @RequestParam(required = false) Long sectorId,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PagedResponse.from(routeService.search(sectorId, name, pageable));
    }

    @GetMapping("/{id}")
    public RouteResponse get(@PathVariable Long id) {
        return routeService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse create(
            @Valid @RequestBody RouteRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        return routeService.create(request, principal);
    }

    @PutMapping("/{id}")
    public RouteResponse update(
            @PathVariable Long id,
            @Valid @RequestBody RouteRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return routeService.update(id, request, principal);
    }

    @PutMapping("/{id}/verification")
    public RouteResponse setVerification(
            @PathVariable Long id,
            @Valid @RequestBody VerificationRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return routeService.setVerified(id, request.verified(), principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        routeService.delete(id, principal);
    }
}
