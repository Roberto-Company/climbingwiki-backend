package com.roberto.climbingwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.dtos.GradingSystemRequest;
import com.roberto.climbingwiki.dtos.GradingSystemResponse;
import com.roberto.climbingwiki.exceptions.GradingSystemConflictException;
import com.roberto.climbingwiki.exceptions.GradingSystemNotFoundException;
import com.roberto.climbingwiki.model.GradingTypes;
import com.roberto.climbingwiki.model.entities.GradingSystem;
import com.roberto.climbingwiki.repositories.GradingSystemRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GradingSystemServiceTest {

    @Mock
    private GradingSystemRepository gradingSystemRepository;

    private GradingSystemService gradingSystemService;

    @BeforeEach
    void setUp() {
        gradingSystemService = new GradingSystemService(gradingSystemRepository);
    }

    private GradingSystem gradingSystem(Long id, String name, GradingTypes.Category category) {
        GradingSystem gradingSystem = new GradingSystem();
        gradingSystem.setId(id);
        gradingSystem.setName(name);
        gradingSystem.setCategory(category);
        return gradingSystem;
    }

    @Test
    void findAll_returnsMappedResponses() {
        when(gradingSystemRepository.findAll())
                .thenReturn(List.of(gradingSystem(1L, "French", GradingTypes.Category.ROPED)));

        List<GradingSystemResponse> result = gradingSystemService.findAll();

        assertThat(result).extracting(GradingSystemResponse::name).containsExactly("French");
    }

    @Test
    void findById_existingSystem_returnsResponse() {
        when(gradingSystemRepository.findById(1L))
                .thenReturn(Optional.of(gradingSystem(1L, "French", GradingTypes.Category.ROPED)));

        GradingSystemResponse result = gradingSystemService.findById(1L);

        assertThat(result.name()).isEqualTo("French");
    }

    @Test
    void findById_missingSystem_throws() {
        when(gradingSystemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradingSystemService.findById(99L))
                .isInstanceOf(GradingSystemNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_duplicateName_throws() {
        when(gradingSystemRepository.findByName("French"))
                .thenReturn(Optional.of(gradingSystem(1L, "French", GradingTypes.Category.ROPED)));

        assertThatThrownBy(() -> gradingSystemService.create(
                        new GradingSystemRequest("French", GradingTypes.Category.ROPED)))
                .isInstanceOf(GradingSystemConflictException.class)
                .hasMessageContaining("French");

        verify(gradingSystemRepository, never()).save(any());
    }

    @Test
    void create_validRequest_savesAndReturnsResponse() {
        when(gradingSystemRepository.findByName("French")).thenReturn(Optional.empty());
        when(gradingSystemRepository.save(any(GradingSystem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GradingSystemResponse result =
                gradingSystemService.create(new GradingSystemRequest("French", GradingTypes.Category.ROPED));

        assertThat(result.name()).isEqualTo("French");
        assertThat(result.category()).isEqualTo(GradingTypes.Category.ROPED);
    }

    @Test
    void update_keepingOwnName_doesNotConflictWithItself() {
        GradingSystem existing = gradingSystem(1L, "French", GradingTypes.Category.ROPED);
        when(gradingSystemRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(gradingSystemRepository.findByName("French")).thenReturn(Optional.of(existing));
        when(gradingSystemRepository.save(any(GradingSystem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GradingSystemResponse result = gradingSystemService.update(
                1L, new GradingSystemRequest("French", GradingTypes.Category.BOULDER));

        assertThat(result.category()).isEqualTo(GradingTypes.Category.BOULDER);
    }

    @Test
    void update_toNameHeldByAnotherSystem_throws() {
        GradingSystem existing = gradingSystem(1L, "French", GradingTypes.Category.ROPED);
        GradingSystem other = gradingSystem(2L, "YDS", GradingTypes.Category.ROPED);
        when(gradingSystemRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(gradingSystemRepository.findByName("YDS")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> gradingSystemService.update(
                        1L, new GradingSystemRequest("YDS", GradingTypes.Category.ROPED)))
                .isInstanceOf(GradingSystemConflictException.class);

        verify(gradingSystemRepository, never()).save(any());
    }

    @Test
    void update_missingSystem_throws() {
        when(gradingSystemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradingSystemService.update(
                        99L, new GradingSystemRequest("French", GradingTypes.Category.ROPED)))
                .isInstanceOf(GradingSystemNotFoundException.class);
    }

    @Test
    void delete_existingSystem_deletes() {
        when(gradingSystemRepository.existsById(1L)).thenReturn(true);

        gradingSystemService.delete(1L);

        verify(gradingSystemRepository).deleteById(1L);
    }

    @Test
    void delete_missingSystem_throwsAndDoesNotDelete() {
        when(gradingSystemRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> gradingSystemService.delete(99L))
                .isInstanceOf(GradingSystemNotFoundException.class);

        verify(gradingSystemRepository, never()).deleteById(99L);
    }
}
