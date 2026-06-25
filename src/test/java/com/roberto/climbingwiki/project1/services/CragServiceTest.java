package com.roberto.climbingwiki.project1.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private CragRepository cragRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private UserRepository userRepository;

    private CragService cragService;

    @BeforeEach
    void setUp() {
        cragService = new CragService(cragRepository, zoneRepository, userRepository);
    }

    private Crag crag(Long id, String name) {
        Crag crag = new Crag();
        crag.setId(id);
        crag.setName(name);
        return crag;
    }

    private CragRequest request(Long zoneId, Long createdById) {
        return new CragRequest(
                zoneId, createdById, "Montserrat", "desc", null, null, null, "S", "limestone", "approach");
    }

    @Test
    void findAll_returnsMappedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Crag> cragPage = new PageImpl<>(List.of(crag(1L, "Montserrat"), crag(2L, "Siurana")), pageable, 2);
        when(cragRepository.findAll(pageable)).thenReturn(cragPage);

        Page<CragResponse> result = cragService.findAll(pageable);

        assertThat(result.getContent()).extracting(CragResponse::name).containsExactly("Montserrat", "Siurana");
    }

    @Test
    void findByZoneId_returnsMappedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Crag> cragPage = new PageImpl<>(List.of(crag(1L, "Montserrat")), pageable, 1);
        when(cragRepository.findByZoneId(7L, pageable)).thenReturn(cragPage);

        Page<CragResponse> result = cragService.findByZoneId(7L, pageable);

        assertThat(result.getContent()).extracting(CragResponse::name).containsExactly("Montserrat");
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
    void create_withoutZoneOrCreator_savesWithNullAssociations() {
        when(cragRepository.save(any(Crag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CragResponse result = cragService.create(request(null, null));

        assertThat(result.zoneId()).isNull();
        assertThat(result.createdById()).isNull();
        verify(zoneRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void create_withZoneId_resolvesZone() {
        Zone zone = new Zone();
        zone.setId(7L);
        when(zoneRepository.findById(7L)).thenReturn(Optional.of(zone));
        when(cragRepository.save(any(Crag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CragResponse result = cragService.create(request(7L, null));

        assertThat(result.zoneId()).isEqualTo(7L);
    }

    @Test
    void create_withMissingZoneId_throws() {
        when(zoneRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cragService.create(request(7L, null)))
                .isInstanceOf(ZoneNotFoundException.class)
                .hasMessageContaining("7");
    }

    @Test
    void create_withMissingCreatedById_throws() {
        when(userRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cragService.create(request(null, 3L)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("3");
    }

    @Test
    void update_existingCrag_appliesChanges() {
        Crag existing = crag(1L, "Old name");
        when(cragRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cragRepository.save(any(Crag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CragResponse result = cragService.update(1L, request(null, null));

        assertThat(result.name()).isEqualTo("Montserrat");
    }

    @Test
    void update_missingCrag_throws() {
        when(cragRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cragService.update(99L, request(null, null)))
                .isInstanceOf(CragNotFoundException.class);
    }

    @Test
    void delete_existingCrag_deletes() {
        when(cragRepository.existsById(1L)).thenReturn(true);

        cragService.delete(1L);

        verify(cragRepository).deleteById(1L);
    }

    @Test
    void delete_missingCrag_throwsAndDoesNotDelete() {
        when(cragRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> cragService.delete(99L)).isInstanceOf(CragNotFoundException.class);

        verify(cragRepository, never()).deleteById(99L);
    }
}
