package com.roberto.climbingwiki.controllers;

import com.roberto.climbingwiki.dtos.ListRouteRequest;
import com.roberto.climbingwiki.dtos.ListRouteResponse;
import com.roberto.climbingwiki.dtos.UpsertResult;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import com.roberto.climbingwiki.services.ListRouteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/list-routes")
public class ListRouteController {

    private final ListRouteService listRouteService;

    public ListRouteController(ListRouteService listRouteService) {
        this.listRouteService = listRouteService;
    }

    @GetMapping
    public List<ListRouteResponse> list(
            @RequestParam(required = false) Long listId, @RequestParam(required = false) Long routeId) {
        if (listId != null) {
            return listRouteService.findByList(listId);
        }
        if (routeId != null) {
            return listRouteService.findByRoute(routeId);
        }
        return listRouteService.findAll();
    }

    @GetMapping("/{listId}/{routeId}")
    public ListRouteResponse get(@PathVariable Long listId, @PathVariable Long routeId) {
        return listRouteService.findOne(listId, routeId);
    }

    @PostMapping
    public ResponseEntity<ListRouteResponse> upsert(
            @Valid @RequestBody ListRouteRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        UpsertResult<ListRouteResponse> result = listRouteService.upsert(request, principal);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.data());
    }

    @DeleteMapping("/{listId}/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long listId,
            @PathVariable Long routeId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        listRouteService.delete(listId, routeId, principal);
    }
}
