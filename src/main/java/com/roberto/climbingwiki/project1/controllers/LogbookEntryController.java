package com.roberto.climbingwiki.project1.controllers;

import com.roberto.climbingwiki.project1.dtos.LogbookEntryRequest;
import com.roberto.climbingwiki.project1.dtos.LogbookEntryResponse;
import com.roberto.climbingwiki.project1.dtos.PagedResponse;
import com.roberto.climbingwiki.project1.security.AuthenticatedUser;
import com.roberto.climbingwiki.project1.services.LogbookEntryService;
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
@RequestMapping("/api/logbook-entries")
public class LogbookEntryController {

    private final LogbookEntryService logbookEntryService;

    public LogbookEntryController(LogbookEntryService logbookEntryService) {
        this.logbookEntryService = logbookEntryService;
    }

    @GetMapping
    public PagedResponse<LogbookEntryResponse> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long routeId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        if (userId != null) {
            return PagedResponse.from(logbookEntryService.findByUser(userId, pageable));
        }
        if (routeId != null) {
            return PagedResponse.from(logbookEntryService.findByRoute(routeId, pageable));
        }
        return PagedResponse.from(logbookEntryService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public LogbookEntryResponse get(@PathVariable Long id) {
        return logbookEntryService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LogbookEntryResponse create(
            @Valid @RequestBody LogbookEntryRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        return logbookEntryService.create(request, principal);
    }

    @PutMapping("/{id}")
    public LogbookEntryResponse update(
            @PathVariable Long id,
            @Valid @RequestBody LogbookEntryRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return logbookEntryService.update(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        logbookEntryService.delete(id, principal);
    }
}
