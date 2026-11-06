package com.roberto.climbingwiki.controllers;

import com.roberto.climbingwiki.dtos.PagedResponse;
import com.roberto.climbingwiki.dtos.PhotoRequest;
import com.roberto.climbingwiki.dtos.PhotoResponse;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import com.roberto.climbingwiki.services.PhotoService;
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
@RequestMapping("/api/photos")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping
    public PagedResponse<PhotoResponse> list(
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long cragId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        if (routeId != null) {
            return PagedResponse.from(photoService.findByRoute(routeId, pageable));
        }
        if (cragId != null) {
            return PagedResponse.from(photoService.findByCrag(cragId, pageable));
        }
        return PagedResponse.from(photoService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public PhotoResponse get(@PathVariable Long id) {
        return photoService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoResponse create(
            @Valid @RequestBody PhotoRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        return photoService.create(request, principal);
    }

    @PutMapping("/{id}")
    public PhotoResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PhotoRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return photoService.update(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        photoService.delete(id, principal);
    }
}
