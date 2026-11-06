package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.dtos.GradingSystemRequest;
import com.roberto.climbingwiki.dtos.GradingSystemResponse;
import com.roberto.climbingwiki.exceptions.GradingSystemConflictException;
import com.roberto.climbingwiki.exceptions.GradingSystemNotFoundException;
import com.roberto.climbingwiki.model.entities.GradingSystem;
import com.roberto.climbingwiki.repositories.GradingSystemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingSystemService {

    private final GradingSystemRepository gradingSystemRepository;

    public GradingSystemService(GradingSystemRepository gradingSystemRepository) {
        this.gradingSystemRepository = gradingSystemRepository;
    }

    public List<GradingSystemResponse> findAll() {
        return gradingSystemRepository.findAll().stream().map(GradingSystemResponse::from).toList();
    }

    public GradingSystemResponse findById(Long id) {
        return gradingSystemRepository.findById(id)
                .map(GradingSystemResponse::from)
                .orElseThrow(() -> new GradingSystemNotFoundException(id));
    }

    @Transactional
    public GradingSystemResponse create(GradingSystemRequest request) {
        GradingSystem gradingSystem = new GradingSystem();
        applyRequest(gradingSystem, request, null);
        return GradingSystemResponse.from(gradingSystemRepository.save(gradingSystem));
    }

    @Transactional
    public GradingSystemResponse update(Long id, GradingSystemRequest request) {
        GradingSystem gradingSystem = gradingSystemRepository.findById(id)
                .orElseThrow(() -> new GradingSystemNotFoundException(id));
        applyRequest(gradingSystem, request, id);
        return GradingSystemResponse.from(gradingSystemRepository.save(gradingSystem));
    }

    @Transactional
    public void delete(Long id) {
        if (!gradingSystemRepository.existsById(id)) {
            throw new GradingSystemNotFoundException(id);
        }
        gradingSystemRepository.deleteById(id);
    }

    private void applyRequest(GradingSystem gradingSystem, GradingSystemRequest request, Long selfId) {
        gradingSystemRepository.findByName(request.name())
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new GradingSystemConflictException(request.name());
                });

        gradingSystem.setName(request.name());
        gradingSystem.setCategory(request.category());
    }
}
