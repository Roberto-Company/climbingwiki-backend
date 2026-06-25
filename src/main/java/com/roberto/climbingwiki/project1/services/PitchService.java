package com.roberto.climbingwiki.project1.services;

import com.roberto.climbingwiki.project1.dtos.PitchRequest;
import com.roberto.climbingwiki.project1.dtos.PitchResponse;
import com.roberto.climbingwiki.project1.exceptions.GradeNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.PitchNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.PitchOrderConflictException;
import com.roberto.climbingwiki.project1.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.Grade;
import com.roberto.climbingwiki.project1.model.entities.Pitch;
import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.repositories.GradeRepository;
import com.roberto.climbingwiki.project1.repositories.PitchRepository;
import com.roberto.climbingwiki.project1.repositories.RouteRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PitchService {

    private final PitchRepository pitchRepository;
    private final RouteRepository routeRepository;
    private final GradeRepository gradeRepository;

    public PitchService(
            PitchRepository pitchRepository, RouteRepository routeRepository, GradeRepository gradeRepository) {
        this.pitchRepository = pitchRepository;
        this.routeRepository = routeRepository;
        this.gradeRepository = gradeRepository;
    }

    public Page<PitchResponse> findAll(Pageable pageable) {
        return pitchRepository.findAll(pageable).map(PitchResponse::from);
    }

    public Page<PitchResponse> findByRouteId(Long routeId, Pageable pageable) {
        return pitchRepository.findByRouteIdOrderByOrderIndexAsc(routeId, pageable).map(PitchResponse::from);
    }

    public PitchResponse findById(Long id) {
        return pitchRepository.findById(id)
                .map(PitchResponse::from)
                .orElseThrow(() -> new PitchNotFoundException(id));
    }

    @Transactional
    public PitchResponse create(PitchRequest request) {
        Pitch pitch = new Pitch();
        applyRequest(pitch, request);
        return PitchResponse.from(pitchRepository.save(pitch));
    }

    @Transactional
    public PitchResponse update(Long id, PitchRequest request) {
        Pitch pitch = pitchRepository.findById(id).orElseThrow(() -> new PitchNotFoundException(id));
        applyRequest(pitch, request);
        return PitchResponse.from(pitchRepository.save(pitch));
    }

    @Transactional
    public void delete(Long id) {
        if (!pitchRepository.existsById(id)) {
            throw new PitchNotFoundException(id);
        }
        pitchRepository.deleteById(id);
    }

    private void applyRequest(Pitch pitch, PitchRequest request) {
        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new RouteNotFoundException(request.routeId()));
        pitch.setRoute(route);

        if (request.gradeId() != null) {
            Grade grade = gradeRepository.findById(request.gradeId())
                    .orElseThrow(() -> new GradeNotFoundException(request.gradeId()));
            pitch.setGrade(grade);
        } else {
            pitch.setGrade(null);
        }

        Optional<Pitch> existingAtOrderIndex =
                pitchRepository.findByRouteIdAndOrderIndex(request.routeId(), request.orderIndex());
        if (existingAtOrderIndex.isPresent() && !existingAtOrderIndex.get().getId().equals(pitch.getId())) {
            throw new PitchOrderConflictException(request.routeId(), request.orderIndex());
        }

        pitch.setOrderIndex(request.orderIndex());
        pitch.setLengthM(request.lengthM());
        pitch.setBoltsCount(request.boltsCount());
        pitch.setDescription(request.description());
    }
}
