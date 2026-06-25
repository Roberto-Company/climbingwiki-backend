package com.roberto.climbingwiki.project1.controllers;

import com.roberto.climbingwiki.project1.dtos.PagedResponse;
import com.roberto.climbingwiki.project1.dtos.TagRequest;
import com.roberto.climbingwiki.project1.dtos.TagResponse;
import com.roberto.climbingwiki.project1.services.TagService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public PagedResponse<TagResponse> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PagedResponse.from(tagService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public TagResponse get(@PathVariable Long id) {
        return tagService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse create(@Valid @RequestBody TagRequest request) {
        return tagService.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tagService.delete(id);
    }
}
