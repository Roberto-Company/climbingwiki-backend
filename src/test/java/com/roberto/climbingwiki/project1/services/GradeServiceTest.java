package com.roberto.climbingwiki.project1.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.project1.dtos.GradeRequest;
import com.roberto.climbingwiki.project1.dtos.GradeResponse;
import com.roberto.climbingwiki.project1.exceptions.GradeConflictException;
import com.roberto.climbingwiki.project1.exceptions.GradeNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.GradingSystemNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.Grade;
import com.roberto.climbingwiki.project1.model.entities.GradingSystem;
import com.roberto.climbingwiki.project1.repositories.GradeRepository;
import com.roberto.climbingwiki.project1.repositories.GradingSystemRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private GradingSystemRepository gradingSystemRepository;

    private GradeService gradeService;

    @BeforeEach
    void setUp() {
        gradeService = new GradeService(gradeRepository, gradingSystemRepository);
    }

    private GradingSystem gradingSystem(Long id) {
        GradingSystem gradingSystem = new GradingSystem();
        gradingSystem.setId(id);
        return gradingSystem;
    }

    private Grade grade(Long id, GradingSystem gradingSystem, String label) {
        Grade grade = new Grade();
        grade.setId(id);
        grade.setGradingSystem(gradingSystem);
        grade.setLabel(label);
        grade.setDifficultyScore(10);
        return grade;
    }

    private GradeRequest request(Long gradingSystemId, String label) {
        return new GradeRequest(gradingSystemId, label, 10);
    }

    @Test
    void findAll_returnsMappedResponses() {
        GradingSystem system = gradingSystem(1L);
        when(gradeRepository.findAll()).thenReturn(List.of(grade(1L, system, "6a"), grade(2L, system, "6a+")));

        List<GradeResponse> result = gradeService.findAll();

        assertThat(result).extracting(GradeResponse::label).containsExactly("6a", "6a+");
    }

    @Test
    void findByGradingSystem_returnsGradesForThatSystem() {
        GradingSystem system = gradingSystem(1L);
        when(gradeRepository.findByGradingSystemId(1L)).thenReturn(List.of(grade(1L, system, "6a")));

        List<GradeResponse> result = gradeService.findByGradingSystem(1L);

        assertThat(result).extracting(GradeResponse::gradingSystemId).containsExactly(1L);
    }

    @Test
    void findById_existingGrade_returnsResponse() {
        when(gradeRepository.findById(1L)).thenReturn(Optional.of(grade(1L, gradingSystem(1L), "6a")));

        GradeResponse result = gradeService.findById(1L);

        assertThat(result.label()).isEqualTo("6a");
    }

    @Test
    void findById_missingGrade_throws() {
        when(gradeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.findById(99L))
                .isInstanceOf(GradeNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_missingGradingSystem_throws() {
        when(gradingSystemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.create(request(1L, "6a")))
                .isInstanceOf(GradingSystemNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void create_duplicateLabelInSameSystem_throws() {
        GradingSystem system = gradingSystem(1L);
        when(gradingSystemRepository.findById(1L)).thenReturn(Optional.of(system));
        when(gradeRepository.findByGradingSystemIdAndLabel(1L, "6a"))
                .thenReturn(Optional.of(grade(5L, system, "6a")));

        assertThatThrownBy(() -> gradeService.create(request(1L, "6a")))
                .isInstanceOf(GradeConflictException.class)
                .hasMessageContaining("6a");

        verify(gradeRepository, never()).save(any());
    }

    @Test
    void create_validRequest_savesAndReturnsResponse() {
        GradingSystem system = gradingSystem(1L);
        when(gradingSystemRepository.findById(1L)).thenReturn(Optional.of(system));
        when(gradeRepository.findByGradingSystemIdAndLabel(1L, "6a")).thenReturn(Optional.empty());
        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GradeResponse result = gradeService.create(request(1L, "6a"));

        assertThat(result.label()).isEqualTo("6a");
        assertThat(result.gradingSystemId()).isEqualTo(1L);
    }

    @Test
    void update_keepingOwnLabel_doesNotConflictWithItself() {
        GradingSystem system = gradingSystem(1L);
        Grade existing = grade(1L, system, "6a");
        when(gradeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(gradingSystemRepository.findById(1L)).thenReturn(Optional.of(system));
        when(gradeRepository.findByGradingSystemIdAndLabel(1L, "6a")).thenReturn(Optional.of(existing));
        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GradeResponse result = gradeService.update(1L, new GradeRequest(1L, "6a", 20));

        assertThat(result.difficultyScore()).isEqualTo(20);
    }

    @Test
    void update_toLabelHeldByAnotherGrade_throws() {
        GradingSystem system = gradingSystem(1L);
        Grade existing = grade(1L, system, "6a");
        Grade other = grade(2L, system, "6a+");
        when(gradeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(gradingSystemRepository.findById(1L)).thenReturn(Optional.of(system));
        when(gradeRepository.findByGradingSystemIdAndLabel(1L, "6a+")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> gradeService.update(1L, request(1L, "6a+")))
                .isInstanceOf(GradeConflictException.class);

        verify(gradeRepository, never()).save(any());
    }

    @Test
    void update_missingGrade_throws() {
        when(gradeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.update(99L, request(1L, "6a")))
                .isInstanceOf(GradeNotFoundException.class);
    }

    @Test
    void delete_existingGrade_deletes() {
        when(gradeRepository.existsById(1L)).thenReturn(true);

        gradeService.delete(1L);

        verify(gradeRepository).deleteById(1L);
    }

    @Test
    void delete_missingGrade_throwsAndDoesNotDelete() {
        when(gradeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> gradeService.delete(99L)).isInstanceOf(GradeNotFoundException.class);

        verify(gradeRepository, never()).deleteById(99L);
    }
}
