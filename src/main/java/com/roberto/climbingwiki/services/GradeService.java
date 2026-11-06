package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.dtos.GradeRequest;
import com.roberto.climbingwiki.dtos.GradeResponse;
import com.roberto.climbingwiki.exceptions.GradeConflictException;
import com.roberto.climbingwiki.exceptions.GradeNotFoundException;
import com.roberto.climbingwiki.exceptions.GradingSystemNotFoundException;
import com.roberto.climbingwiki.model.entities.Grade;
import com.roberto.climbingwiki.model.entities.GradingSystem;
import com.roberto.climbingwiki.repositories.GradeRepository;
import com.roberto.climbingwiki.repositories.GradingSystemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradeService {

    private final GradeRepository gradeRepository;
    private final GradingSystemRepository gradingSystemRepository;

    public GradeService(GradeRepository gradeRepository, GradingSystemRepository gradingSystemRepository) {
        this.gradeRepository = gradeRepository;
        this.gradingSystemRepository = gradingSystemRepository;
    }

    public List<GradeResponse> findAll() {
        return gradeRepository.findAll().stream().map(GradeResponse::from).toList();
    }

    public List<GradeResponse> findByGradingSystem(Long gradingSystemId) {
        return gradeRepository.findByGradingSystemId(gradingSystemId).stream()
                .map(GradeResponse::from)
                .toList();
    }

    public GradeResponse findById(Long id) {
        return gradeRepository.findById(id)
                .map(GradeResponse::from)
                .orElseThrow(() -> new GradeNotFoundException(id));
    }

    @Transactional
    public GradeResponse create(GradeRequest request) {
        Grade grade = new Grade();
        applyRequest(grade, request, null);
        return GradeResponse.from(gradeRepository.save(grade));
    }

    @Transactional
    public GradeResponse update(Long id, GradeRequest request) {
        Grade grade = gradeRepository.findById(id).orElseThrow(() -> new GradeNotFoundException(id));
        applyRequest(grade, request, id);
        return GradeResponse.from(gradeRepository.save(grade));
    }

    @Transactional
    public void delete(Long id) {
        if (!gradeRepository.existsById(id)) {
            throw new GradeNotFoundException(id);
        }
        gradeRepository.deleteById(id);
    }

    private void applyRequest(Grade grade, GradeRequest request, Long selfId) {
        GradingSystem gradingSystem = gradingSystemRepository.findById(request.gradingSystemId())
                .orElseThrow(() -> new GradingSystemNotFoundException(request.gradingSystemId()));

        gradeRepository.findByGradingSystemIdAndLabel(request.gradingSystemId(), request.label())
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new GradeConflictException(request.gradingSystemId(), request.label());
                });

        grade.setGradingSystem(gradingSystem);
        grade.setLabel(request.label());
        grade.setDifficultyScore(request.difficultyScore());
    }
}
