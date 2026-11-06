package com.roberto.climbingwiki.controllers;

import com.roberto.climbingwiki.dtos.CragRequest;
import com.roberto.climbingwiki.dtos.CragResponse;
import com.roberto.climbingwiki.dtos.VerificationRequest;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import com.roberto.climbingwiki.dtos.PagedResponse;
import com.roberto.climbingwiki.services.CragService;
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
@RequestMapping("/api/crags")
public class CragController {

    private final CragService cragService;

    public CragController(CragService cragService) {
        this.cragService = cragService;
    }

    @GetMapping
    public PagedResponse<CragResponse> list(
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PagedResponse.from(cragService.search(zoneId, name, pageable));
    }

    @GetMapping("/{id}")
    public CragResponse get(@PathVariable Long id) {
        return cragService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CragResponse create(
            @Valid @RequestBody CragRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        return cragService.create(request, principal);
    }

    @PutMapping("/{id}")
    public CragResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CragRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return cragService.update(id, request, principal);
    }

    @PutMapping("/{id}/verification")
    public CragResponse setVerification(
            @PathVariable Long id,
            @Valid @RequestBody VerificationRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return cragService.setVerified(id, request.verified(), principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        cragService.delete(id, principal);
    }
}
