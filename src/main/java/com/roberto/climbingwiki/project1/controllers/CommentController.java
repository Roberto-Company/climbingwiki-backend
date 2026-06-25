package com.roberto.climbingwiki.project1.controllers;

import com.roberto.climbingwiki.project1.dtos.CommentRequest;
import com.roberto.climbingwiki.project1.dtos.CommentResponse;
import com.roberto.climbingwiki.project1.dtos.PagedResponse;
import com.roberto.climbingwiki.project1.security.AuthenticatedUser;
import com.roberto.climbingwiki.project1.services.CommentService;
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
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public PagedResponse<CommentResponse> list(
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long cragId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        if (routeId != null) {
            return PagedResponse.from(commentService.findByRoute(routeId, pageable));
        }
        if (cragId != null) {
            return PagedResponse.from(commentService.findByCrag(cragId, pageable));
        }
        return PagedResponse.from(commentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public CommentResponse get(@PathVariable Long id) {
        return commentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @Valid @RequestBody CommentRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        return commentService.create(request, principal);
    }

    @PutMapping("/{id}")
    public CommentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return commentService.update(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        commentService.delete(id, principal);
    }
}
