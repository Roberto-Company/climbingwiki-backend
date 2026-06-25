package com.roberto.climbingwiki.project1.controllers;

import com.roberto.climbingwiki.project1.dtos.PagedResponse;
import com.roberto.climbingwiki.project1.dtos.PitchRequest;
import com.roberto.climbingwiki.project1.dtos.PitchResponse;
import com.roberto.climbingwiki.project1.services.PitchService;
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
@RequestMapping("/api/pitches")
public class PitchController {

    private final PitchService pitchService;

    public PitchController(PitchService pitchService) {
        this.pitchService = pitchService;
    }

    @GetMapping
    public PagedResponse<PitchResponse> list(
            @RequestParam(required = false) Long routeId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PagedResponse.from(
                routeId != null ? pitchService.findByRouteId(routeId, pageable) : pitchService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public PitchResponse get(@PathVariable Long id) {
        return pitchService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PitchResponse create(@Valid @RequestBody PitchRequest request) {
        return pitchService.create(request);
    }

    @PutMapping("/{id}")
    public PitchResponse update(@PathVariable Long id, @Valid @RequestBody PitchRequest request) {
        return pitchService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        pitchService.delete(id);
    }
}
