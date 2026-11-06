package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.repositories.CatalogSpecifications;
import com.roberto.climbingwiki.dtos.CragRequest;
import com.roberto.climbingwiki.dtos.CragResponse;
import com.roberto.climbingwiki.exceptions.CragNotFoundException;
import com.roberto.climbingwiki.exceptions.ZoneNotFoundException;
import com.roberto.climbingwiki.model.entities.Crag;
import com.roberto.climbingwiki.model.entities.Zone;
import com.roberto.climbingwiki.repositories.CragRepository;
import com.roberto.climbingwiki.repositories.SectorRepository;
import com.roberto.climbingwiki.repositories.ZoneRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CragService {

    private final CragRepository cragRepository;
    private final ZoneRepository zoneRepository;
    private final SectorRepository sectorRepository;
    private final CatalogAccess access;

    public CragService(
            CragRepository cragRepository,
            ZoneRepository zoneRepository,
            SectorRepository sectorRepository,
            CatalogAccess access) {
        this.cragRepository = cragRepository;
        this.zoneRepository = zoneRepository;
        this.sectorRepository = sectorRepository;
        this.access = access;
    }

    /** Lists crags matching every given filter; null or blank filters are ignored. */
    public Page<CragResponse> search(Long zoneId, String name, Pageable pageable) {
        return cragRepository
                .findAll(CatalogSpecifications.cragFilter(zoneId, name), pageable)
                .map(CragResponse::from);
    }

    public CragResponse findById(Long id) {
        return cragRepository.findById(id)
                .map(CragResponse::from)
                .orElseThrow(() -> new CragNotFoundException(id));
    }

    @Transactional
    public CragResponse create(CragRequest request, AuthenticatedUser requester) {
        Crag crag = new Crag();
        access.stampCreation(crag, requester);
        applyRequest(crag, request);
        return CragResponse.from(cragRepository.save(crag));
    }

    @Transactional
    public CragResponse update(Long id, CragRequest request, AuthenticatedUser requester) {
        Crag crag = getCrag(id);
        access.checkCanEdit(crag, requester);
        applyRequest(crag, request);
        return CragResponse.from(cragRepository.save(crag));
    }

    @Transactional
    public void delete(Long id, AuthenticatedUser requester) {
        Crag crag = getCrag(id);
        access.checkCanDelete(crag, requester, sectorRepository.existsByCragId(id));
        cragRepository.deleteById(id);
    }

    @Transactional
    public CragResponse setVerified(Long id, boolean verified, AuthenticatedUser requester) {
        Crag crag = getCrag(id);
        access.setVerified(crag, verified, requester);
        Crag saved = cragRepository.save(crag);
        return CragResponse.from(saved);
    }

    private Crag getCrag(Long id) {
        return cragRepository.findById(id).orElseThrow(() -> new CragNotFoundException(id));
    }

    private void applyRequest(Crag crag, CragRequest request) {
        if (request.zoneId() != null) {
            Zone zone = zoneRepository.findById(request.zoneId())
                    .orElseThrow(() -> new ZoneNotFoundException(request.zoneId()));
            crag.setZone(zone);
        } else {
            crag.setZone(null);
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
