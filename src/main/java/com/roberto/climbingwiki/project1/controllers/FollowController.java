package com.roberto.climbingwiki.project1.controllers;

import com.roberto.climbingwiki.project1.dtos.FollowRequest;
import com.roberto.climbingwiki.project1.dtos.FollowResponse;
import com.roberto.climbingwiki.project1.dtos.UpsertResult;
import com.roberto.climbingwiki.project1.security.AuthenticatedUser;
import com.roberto.climbingwiki.project1.services.FollowService;
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
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @GetMapping
    public List<FollowResponse> list(
            @RequestParam(required = false) Long followerId, @RequestParam(required = false) Long followedId) {
        if (followerId != null) {
            return followService.findByFollower(followerId);
        }
        if (followedId != null) {
            return followService.findByFollowed(followedId);
        }
        return followService.findAll();
    }

    @GetMapping("/{followerId}/{followedId}")
    public FollowResponse get(@PathVariable Long followerId, @PathVariable Long followedId) {
        return followService.findOne(followerId, followedId);
    }

    @PostMapping
    public ResponseEntity<FollowResponse> upsert(
            @Valid @RequestBody FollowRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        UpsertResult<FollowResponse> result = followService.upsert(request, principal);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.data());
    }

    @DeleteMapping("/{followerId}/{followedId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long followerId,
            @PathVariable Long followedId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        followService.delete(followerId, followedId, principal);
    }
}
