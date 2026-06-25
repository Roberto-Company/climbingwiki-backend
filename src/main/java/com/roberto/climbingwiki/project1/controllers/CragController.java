package com.roberto.climbingwiki.project1.controllers;

import com.roberto.climbingwiki.project1.dtos.CragRequest;
import com.roberto.climbingwiki.project1.dtos.CragResponse;
import com.roberto.climbingwiki.project1.dtos.PagedResponse;
import com.roberto.climbingwiki.project1.services.CragService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
            @RequestParam(required = false) Long zoneId, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PagedResponse.from(
                zoneId != null ? cragService.findByZoneId(zoneId, pageable) : cragService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public CragResponse get(@PathVariable Long id) {
        return cragService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CragResponse create(@Valid @RequestBody CragRequest request) {
        return cragService.create(request);
    }

    @PutMapping("/{id}")
    public CragResponse update(@PathVariable Long id, @Valid @RequestBody CragRequest request) {
        return cragService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        cragService.delete(id);
    }
}
