package com.roberto.climbingwiki.project1.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.project1.model.GradingTypes;
import com.roberto.climbingwiki.project1.model.entities.Grade;
import com.roberto.climbingwiki.project1.model.entities.GradingSystem;
import com.roberto.climbingwiki.project1.repositories.GradeRepository;
import com.roberto.climbingwiki.project1.repositories.GradingSystemRepository;
import java.io.StringReader;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GradeSeedServiceTest {

    @Mock
    private GradingSystemRepository gradingSystemRepository;

    @Mock
    private GradeRepository gradeRepository;

    private GradeSeedService seedService;

    @BeforeEach
    void setUp() {
        seedService = new GradeSeedService(gradingSystemRepository, gradeRepository);
    }

    @Test
    void seed_newSystemAndGrades_createsBoth() {
        String csv =
                """
                grading_system,category,label,difficulty_score
                French,ROPED,6a,8
                French,ROPED,6a+,9
                """;
        when(gradingSystemRepository.findByName("French")).thenReturn(Optional.empty());
        GradingSystem saved = new GradingSystem();
        saved.setId(1L);
        when(gradingSystemRepository.save(any(GradingSystem.class))).thenReturn(saved);
        when(gradeRepository.findByGradingSystemIdAndLabel(1L, "6a")).thenReturn(Optional.empty());
        when(gradeRepository.findByGradingSystemIdAndLabel(1L, "6a+")).thenReturn(Optional.empty());

        GradeSeedResult result = seedService.seed(new StringReader(csv));

        assertThat(result.systemsCreated()).isEqualTo(1);
        assertThat(result.systemsExisting()).isZero();
        assertThat(result.gradesCreated()).isEqualTo(2);
        assertThat(result.gradesExisting()).isZero();
        verify(gradingSystemRepository).save(any(GradingSystem.class));
    }

    @Test
    void seed_existingSystemAndGrade_skipsRecreationAndPreservesScore() {
        String csv =
                """
                grading_system,category,label,difficulty_score
                French,ROPED,6a,999
                """;
        GradingSystem existingSystem = new GradingSystem();
        existingSystem.setId(5L);
        when(gradingSystemRepository.findByName("French")).thenReturn(Optional.of(existingSystem));
        Grade existingGrade = new Grade();
        existingGrade.setDifficultyScore(8);
        when(gradeRepository.findByGradingSystemIdAndLabel(5L, "6a")).thenReturn(Optional.of(existingGrade));

        GradeSeedResult result = seedService.seed(new StringReader(csv));

        assertThat(result.systemsCreated()).isZero();
        assertThat(result.systemsExisting()).isEqualTo(1);
        assertThat(result.gradesCreated()).isZero();
        assertThat(result.gradesExisting()).isEqualTo(1);
        verify(gradingSystemRepository, never()).save(any());
        verify(gradeRepository, never()).save(any());
        assertThat(existingGrade.getDifficultyScore()).isEqualTo(8);
    }

    @Test
    void seed_categoryColumn_mapsToEnum() {
        String csv =
                """
                grading_system,category,label,difficulty_score
                V-Scale,BOULDER,V0,2
                """;
        when(gradingSystemRepository.findByName("V-Scale")).thenReturn(Optional.empty());
        when(gradingSystemRepository.save(any(GradingSystem.class))).thenAnswer(invocation -> {
            GradingSystem system = invocation.getArgument(0);
            system.setId(7L);
            return system;
        });
        when(gradeRepository.findByGradingSystemIdAndLabel(7L, "V0")).thenReturn(Optional.empty());

        seedService.seed(new StringReader(csv));

        org.mockito.ArgumentCaptor<GradingSystem> captor =
                org.mockito.ArgumentCaptor.forClass(GradingSystem.class);
        verify(gradingSystemRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(GradingTypes.Category.BOULDER);
    }
}
