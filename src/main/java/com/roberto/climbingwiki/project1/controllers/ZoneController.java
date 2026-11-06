package com.roberto.climbingwiki.project1.controllers;

import com.roberto.climbingwiki.project1.dtos.PagedResponse;
import com.roberto.climbingwiki.project1.dtos.ZoneRequest;
import com.roberto.climbingwiki.project1.dtos.ZoneResponse;
import com.roberto.climbingwiki.project1.services.ZoneService;
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
@RequestMapping("/api/zones")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @GetMapping
    public PagedResponse<ZoneResponse> list(
            @RequestParam(required = false) Long parentZoneId,
            @RequestParam(required = false, defaultValue = "false") boolean rootOnly,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PagedResponse.from(zoneService.search(parentZoneId, rootOnly, name, pageable));
    }

    @GetMapping("/{id}")
    public ZoneResponse get(@PathVariable Long id) {
        return zoneService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneResponse create(@Valid @RequestBody ZoneRequest request) {
        return zoneService.create(request);
    }

    @PutMapping("/{id}")
    public ZoneResponse update(@PathVariable Long id, @Valid @RequestBody ZoneRequest request) {
        return zoneService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        zoneService.delete(id);
    }
}
