package com.roberto.climbingwiki.project1.controllers;

import com.roberto.climbingwiki.project1.dtos.GradingSystemRequest;
import com.roberto.climbingwiki.project1.dtos.GradingSystemResponse;
import com.roberto.climbingwiki.project1.services.GradingSystemService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/grading-systems")
public class GradingSystemController {

    private final GradingSystemService gradingSystemService;

    public GradingSystemController(GradingSystemService gradingSystemService) {
        this.gradingSystemService = gradingSystemService;
    }

    @GetMapping
    public List<GradingSystemResponse> list() {
        return gradingSystemService.findAll();
    }

    @GetMapping("/{id}")
    public GradingSystemResponse get(@PathVariable Long id) {
        return gradingSystemService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradingSystemResponse create(@Valid @RequestBody GradingSystemRequest request) {
        return gradingSystemService.create(request);
    }

    @PutMapping("/{id}")
    public GradingSystemResponse update(@PathVariable Long id, @Valid @RequestBody GradingSystemRequest request) {
        return gradingSystemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        gradingSystemService.delete(id);
    }
}
