package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.dtos.PitchRequest;
import com.roberto.climbingwiki.dtos.PitchResponse;
import com.roberto.climbingwiki.exceptions.GradeNotFoundException;
import com.roberto.climbingwiki.exceptions.PitchNotFoundException;
import com.roberto.climbingwiki.exceptions.PitchOrderConflictException;
import com.roberto.climbingwiki.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.model.entities.Grade;
import com.roberto.climbingwiki.model.entities.Pitch;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.repositories.GradeRepository;
import com.roberto.climbingwiki.repositories.PitchRepository;
import com.roberto.climbingwiki.repositories.RouteRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
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
    private final CatalogAccess access;

    public PitchService(
            PitchRepository pitchRepository,
            RouteRepository routeRepository,
            GradeRepository gradeRepository,
            CatalogAccess access) {
        this.pitchRepository = pitchRepository;
        this.routeRepository = routeRepository;
        this.gradeRepository = gradeRepository;
        this.access = access;
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
    public PitchResponse create(PitchRequest request, AuthenticatedUser requester) {
        Pitch pitch = new Pitch();
        access.stampCreation(pitch, requester);
        applyRequest(pitch, request);
        return PitchResponse.from(pitchRepository.save(pitch));
    }

    @Transactional
    public PitchResponse update(Long id, PitchRequest request, AuthenticatedUser requester) {
        Pitch pitch = getPitch(id);
        access.checkCanEdit(pitch, requester);
        applyRequest(pitch, request);
        return PitchResponse.from(pitchRepository.save(pitch));
    }

    @Transactional
    public void delete(Long id, AuthenticatedUser requester) {
        Pitch pitch = getPitch(id);
        access.checkCanDelete(pitch, requester, false);
        pitchRepository.deleteById(id);
    }

    @Transactional
    public PitchResponse setVerified(Long id, boolean verified, AuthenticatedUser requester) {
        Pitch pitch = getPitch(id);
        access.setVerified(pitch, verified, requester);
        Pitch saved = pitchRepository.save(pitch);
        return PitchResponse.from(saved);
    }

    private Pitch getPitch(Long id) {
        return pitchRepository.findById(id).orElseThrow(() -> new PitchNotFoundException(id));
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
