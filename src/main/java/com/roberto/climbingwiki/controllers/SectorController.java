package com.roberto.climbingwiki.controllers;

import com.roberto.climbingwiki.dtos.PagedResponse;
import com.roberto.climbingwiki.dtos.SectorRequest;
import com.roberto.climbingwiki.dtos.SectorResponse;
import com.roberto.climbingwiki.services.SectorService;
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
@RequestMapping("/api/sectors")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @GetMapping
    public PagedResponse<SectorResponse> list(
            @RequestParam(required = false) Long cragId, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PagedResponse.from(
                cragId != null ? sectorService.findByCragId(cragId, pageable) : sectorService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public SectorResponse get(@PathVariable Long id) {
        return sectorService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SectorResponse create(@Valid @RequestBody SectorRequest request) {
        return sectorService.create(request);
    }

    @PutMapping("/{id}")
    public SectorResponse update(@PathVariable Long id, @Valid @RequestBody SectorRequest request) {
        return sectorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        sectorService.delete(id);
    }
}
