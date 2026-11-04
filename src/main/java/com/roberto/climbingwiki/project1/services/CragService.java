package com.roberto.climbingwiki.project1.services;

import com.roberto.climbingwiki.project1.dtos.CragRequest;
import com.roberto.climbingwiki.project1.dtos.CragResponse;
import com.roberto.climbingwiki.project1.exceptions.CragNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.ZoneNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.Crag;
import com.roberto.climbingwiki.project1.model.entities.User;
import com.roberto.climbingwiki.project1.model.entities.Zone;
import com.roberto.climbingwiki.project1.repositories.CragRepository;
import com.roberto.climbingwiki.project1.repositories.UserRepository;
import com.roberto.climbingwiki.project1.repositories.ZoneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CragService {

    private final CragRepository cragRepository;
    private final ZoneRepository zoneRepository;
    private final UserRepository userRepository;

    public CragService(CragRepository cragRepository, ZoneRepository zoneRepository, UserRepository userRepository) {
        this.cragRepository = cragRepository;
        this.zoneRepository = zoneRepository;
        this.userRepository = userRepository;
    }

    public Page<CragResponse> findAll(Pageable pageable) {
        return cragRepository.findAll(pageable).map(CragResponse::from);
    }

    public Page<CragResponse> findByZoneId(Long zoneId, Pageable pageable) {
        return cragRepository.findByZoneId(zoneId, pageable).map(CragResponse::from);
    }

    public Page<CragResponse> findByName(String name, Pageable pageable) {
        return cragRepository.findByNameContainingIgnoreCase(name, pageable).map(CragResponse::from);
    }

    public CragResponse findById(Long id) {
        return cragRepository.findById(id)
                .map(CragResponse::from)
                .orElseThrow(() -> new CragNotFoundException(id));
    }

    @Transactional
    public CragResponse create(CragRequest request) {
        Crag crag = new Crag();
        applyRequest(crag, request);
        return CragResponse.from(cragRepository.save(crag));
    }

    @Transactional
    public CragResponse update(Long id, CragRequest request) {
        Crag crag = cragRepository.findById(id).orElseThrow(() -> new CragNotFoundException(id));
        applyRequest(crag, request);
        return CragResponse.from(cragRepository.save(crag));
    }

    @Transactional
    public void delete(Long id) {
        if (!cragRepository.existsById(id)) {
            throw new CragNotFoundException(id);
        }
        cragRepository.deleteById(id);
    }

    private void applyRequest(Crag crag, CragRequest request) {
        if (request.zoneId() != null) {
            Zone zone = zoneRepository.findById(request.zoneId())
                    .orElseThrow(() -> new ZoneNotFoundException(request.zoneId()));
            crag.setZone(zone);
        } else {
            crag.setZone(null);
        }

        if (request.createdById() != null) {
            User createdBy = userRepository.findById(request.createdById())
                    .orElseThrow(() -> new UserNotFoundException(request.createdById()));
            crag.setCreatedBy(createdBy);
        } else {
            crag.setCreatedBy(null);
        }

        crag.setName(request.name());
        crag.setDescription(request.description());
        crag.setLatitude(request.latitude());
        crag.setLongitude(request.longitude());
        crag.setAltitudeM(request.altitudeM());
        crag.setOrientation(request.orientation());
        crag.setRockType(request.rockType());
        crag.setApproachDescription(request.approachDescription());
    }
}
