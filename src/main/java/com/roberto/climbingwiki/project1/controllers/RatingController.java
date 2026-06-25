package com.roberto.climbingwiki.project1.controllers;

import com.roberto.climbingwiki.project1.dtos.RatingRequest;
import com.roberto.climbingwiki.project1.dtos.RatingResponse;
import com.roberto.climbingwiki.project1.dtos.UpsertResult;
import com.roberto.climbingwiki.project1.security.AuthenticatedUser;
import com.roberto.climbingwiki.project1.services.RatingService;
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
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping
    public List<RatingResponse> list(
            @RequestParam(required = false) Long routeId, @RequestParam(required = false) Long userId) {
        if (routeId != null) {
            return ratingService.findByRoute(routeId);
        }
        if (userId != null) {
            return ratingService.findByUser(userId);
        }
        return ratingService.findAll();
    }

    @GetMapping("/{userId}/{routeId}")
    public RatingResponse get(@PathVariable Long userId, @PathVariable Long routeId) {
        return ratingService.findOne(userId, routeId);
    }

    @PostMapping
    public ResponseEntity<RatingResponse> upsert(
            @Valid @RequestBody RatingRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        UpsertResult<RatingResponse> result = ratingService.upsert(request, principal);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.data());
    }

    @DeleteMapping("/{userId}/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long userId,
            @PathVariable Long routeId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        ratingService.delete(userId, routeId, principal);
    }
}
