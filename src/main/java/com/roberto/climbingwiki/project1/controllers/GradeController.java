package com.roberto.climbingwiki.project1.controllers;

import com.roberto.climbingwiki.project1.dtos.GradeRequest;
import com.roberto.climbingwiki.project1.dtos.GradeResponse;
import com.roberto.climbingwiki.project1.services.GradeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping
    public List<GradeResponse> list(@RequestParam(required = false) Long gradingSystemId) {
        return gradingSystemId != null
                ? gradeService.findByGradingSystem(gradingSystemId)
                : gradeService.findAll();
    }

    @GetMapping("/{id}")
    public GradeResponse get(@PathVariable Long id) {
        return gradeService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradeResponse create(@Valid @RequestBody GradeRequest request) {
        return gradeService.create(request);
    }

    @PutMapping("/{id}")
    public GradeResponse update(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
        return gradeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        gradeService.delete(id);
    }
}
