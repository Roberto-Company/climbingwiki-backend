package com.roberto.climbingwiki.services;

import org.springframework.data.jpa.domain.Specification;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.security.AuthenticatedUser;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.dtos.CragRequest;
import com.roberto.climbingwiki.dtos.CragResponse;
import com.roberto.climbingwiki.exceptions.CragNotFoundException;
import com.roberto.climbingwiki.exceptions.ZoneNotFoundException;
import com.roberto.climbingwiki.model.entities.Crag;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.model.entities.Zone;
import com.roberto.climbingwiki.repositories.SectorRepository;
import com.roberto.climbingwiki.repositories.CragRepository;
import com.roberto.climbingwiki.repositories.ZoneRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CragServiceTest {

    private static final AuthenticatedUser REQUESTER =
            new AuthenticatedUser(1L, "climber", User.Role.USER);

    @Mock
    private CatalogAccess access;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private CragRepository cragRepository;

    @Mock
    private ZoneRepository zoneRepository;

    private CragService cragService;

    @BeforeEach
    void setUp() {
        cragService = new CragService(cragRepository, zoneRepository, sectorRepository, access);
    }

    private Crag crag(Long id, String name) {
        Crag crag = new Crag();
        crag.setId(id);
        crag.setName(name);
        return crag;
    }

    private CragRequest request(Long zoneId) {
        return new CragRequest(
                zoneId, "Montserrat", "desc", null, null, null, "S", "limestone", "approach");
    }

    @Test
    void search_returnsMappedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Crag> cragPage = new PageImpl<>(List.of(crag(1L, "Montserrat"), crag(2L, "Siurana")), pageable, 2);
        when(cragRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Crag>>any(), eq(pageable)))
                .thenReturn(cragPage);

        Page<CragResponse> result = cragService.search(7L, "mont", pageable);

        assertThat(result.getContent()).extracting(CragResponse::name).containsExactly("Montserrat", "Siurana");
    }

    @Test
    void findById_existingCrag_returnsResponse() {
        when(cragRepository.findById(1L)).thenReturn(Optional.of(crag(1L, "Montserrat")));

        CragResponse result = cragService.findById(1L);

        assertThat(result.name()).isEqualTo("Montserrat");
    }

    @Test
    void findById_missingCrag_throws() {
        when(cragRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cragService.findById(99L))
                .isInstanceOf(CragNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_withoutZone_savesWithoutZoneAndStampsCreator() {
        when(cragRepository.save(any(Crag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CragResponse result = cragService.create(request(null), REQUESTER);

        assertThat(result.zoneId()).isNull();
        verify(zoneRepository, never()).findById(any());
        verify(access).stampCreation(any(Crag.class), eq(REQUESTER));
    }

    @Test
    void create_withZoneId_resolvesZone() {
        Zone zone = new Zone();
        zone.setId(7L);
        when(zoneRepository.findById(7L)).thenReturn(Optional.of(zone));
        when(cragRepository.save(any(Crag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CragResponse result = cragService.create(request(7L), REQUESTER);

        assertThat(result.zoneId()).isEqualTo(7L);
    }

    @Test
    void create_withMissingZoneId_throws() {
        when(zoneRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cragService.create(request(7L), REQUESTER))
                .isInstanceOf(ZoneNotFoundException.class)
                .hasMessageContaining("7");
    }


    @Test
    void update_existingCrag_appliesChanges() {
        Crag existing = crag(1L, "Old name");
        when(cragRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cragRepository.save(any(Crag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CragResponse result = cragService.update(1L, request(null), REQUESTER);

        assertThat(result.name()).isEqualTo("Montserrat");
    }

    @Test
    void update_missingCrag_throws() {
        when(cragRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cragService.update(99L, request(null), REQUESTER))
                .isInstanceOf(CragNotFoundException.class);
    }

    @Test
    void delete_existingCrag_deletes() {
        when(cragRepository.findById(1L)).thenReturn(Optional.of(new Crag()));

        cragService.delete(1L, REQUESTER);

        verify(cragRepository).deleteById(1L);
    }

    @Test
    void delete_missingCrag_throwsAndDoesNotDelete() {
        when(cragRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cragService.delete(99L, REQUESTER)).isInstanceOf(CragNotFoundException.class);

        verify(cragRepository, never()).deleteById(99L);
    }

    // ----- access rules (CatalogAccess is mocked; its own rules are covered by CatalogAccessTest)

    @Test
    void update_whenAccessDenied_throwsAndDoesNotSave() {
        Crag existing = new Crag();
        existing.setId(1L);
        when(cragRepository.findById(1L)).thenReturn(Optional.of(existing));
        doThrow(new ForbiddenException()).when(access).checkCanEdit(existing, REQUESTER);

        assertThatThrownBy(() -> cragService.update(1L, request(null), REQUESTER)).isInstanceOf(ForbiddenException.class);

        verify(cragRepository, never()).save(any());
    }

    @Test
    void delete_passesChildDataFlagToAccessRules() {
        Crag existing = new Crag();
        existing.setId(1L);
        when(cragRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sectorRepository.existsByCragId(1L)).thenReturn(true);

        cragService.delete(1L, REQUESTER);

        verify(access).checkCanDelete(existing, REQUESTER, true);
        verify(cragRepository).deleteById(1L);
    }

    @Test
    void delete_whenAccessDenied_doesNotDelete() {
        Crag existing = new Crag();
        existing.setId(1L);
        when(cragRepository.findById(1L)).thenReturn(Optional.of(existing));
        doThrow(new ForbiddenException()).when(access).checkCanDelete(any(), eq(REQUESTER), anyBoolean());

        assertThatThrownBy(() -> cragService.delete(1L, REQUESTER)).isInstanceOf(ForbiddenException.class);

        verify(cragRepository, never()).deleteById(any());
    }

    @Test
    void setVerified_appliesAccessRulesAndSaves() {
        Crag existing = new Crag();
        existing.setId(1L);
        when(cragRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cragRepository.save(existing)).thenReturn(existing);

        cragService.setVerified(1L, true, REQUESTER);

        verify(access).setVerified(existing, true, REQUESTER);
        verify(cragRepository).save(existing);
    }
}
