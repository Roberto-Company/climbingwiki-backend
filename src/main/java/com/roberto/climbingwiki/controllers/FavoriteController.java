package com.roberto.climbingwiki.controllers;

import com.roberto.climbingwiki.dtos.FavoriteRequest;
import com.roberto.climbingwiki.dtos.FavoriteResponse;
import com.roberto.climbingwiki.dtos.UpsertResult;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import com.roberto.climbingwiki.services.FavoriteService;
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
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<FavoriteResponse> list(
            @RequestParam(required = false) Long routeId, @RequestParam(required = false) Long userId) {
        if (routeId != null) {
            return favoriteService.findByRoute(routeId);
        }
        if (userId != null) {
            return favoriteService.findByUser(userId);
        }
        return favoriteService.findAll();
    }

    @GetMapping("/{userId}/{routeId}")
    public FavoriteResponse get(@PathVariable Long userId, @PathVariable Long routeId) {
        return favoriteService.findOne(userId, routeId);
    }

    @PostMapping
    public ResponseEntity<FavoriteResponse> upsert(
            @Valid @RequestBody FavoriteRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        UpsertResult<FavoriteResponse> result = favoriteService.upsert(request, principal);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.data());
    }

    @DeleteMapping("/{userId}/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long userId,
            @PathVariable Long routeId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        favoriteService.delete(userId, routeId, principal);
    }
}
