package com.roberto.climbingwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class PitchServiceTest {

    @Mock
    private PitchRepository pitchRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private GradeRepository gradeRepository;

    private PitchService pitchService;

    @BeforeEach
    void setUp() {
        pitchService = new PitchService(pitchRepository, routeRepository, gradeRepository);
    }

    private Route route(Long id) {
        Route route = new Route();
        route.setId(id);
        return route;
    }

    private Pitch pitch(Long id, Route route, Integer orderIndex) {
        Pitch pitch = new Pitch();
        pitch.setId(id);
        pitch.setRoute(route);
        pitch.setOrderIndex(orderIndex);
        return pitch;
    }

    private PitchRequest request(Long routeId, Long gradeId, Integer orderIndex) {
        return new PitchRequest(routeId, gradeId, orderIndex, 25, 8, "crux on the top third");
    }

    @Test
    void findAll_returnsMappedResponses() {
        Route route = route(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Pitch> pitchPage = new PageImpl<>(List.of(pitch(1L, route, 1), pitch(2L, route, 2)), pageable, 2);
        when(pitchRepository.findAll(pageable)).thenReturn(pitchPage);

        Page<PitchResponse> result = pitchService.findAll(pageable);

        assertThat(result.getContent()).extracting(PitchResponse::orderIndex).containsExactly(1, 2);
    }

    @Test
    void findByRouteId_returnsPitchesInOrder() {
        Route route = route(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Pitch> pitchPage = new PageImpl<>(List.of(pitch(1L, route, 1), pitch(2L, route, 2)), pageable, 2);
        when(pitchRepository.findByRouteIdOrderByOrderIndexAsc(1L, pageable)).thenReturn(pitchPage);

        Page<PitchResponse> result = pitchService.findByRouteId(1L, pageable);

        assertThat(result.getContent()).extracting(PitchResponse::id).containsExactly(1L, 2L);
    }

    @Test
    void findById_existingPitch_returnsResponse() {
        when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch(1L, route(1L), 1)));

        PitchResponse result = pitchService.findById(1L);

        assertThat(result.routeId()).isEqualTo(1L);
    }

    @Test
    void findById_missingPitch_throws() {
        when(pitchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pitchService.findById(99L))
                .isInstanceOf(PitchNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_missingRoute_throws() {
        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pitchService.create(request(1L, null, 1)))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void create_missingGrade_throws() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(gradeRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pitchService.create(request(1L, 7L, 1)))
                .isInstanceOf(GradeNotFoundException.class)
                .hasMessageContaining("7");
    }

    @Test
    void create_withValidRouteAndGrade_savesPitch() {
        Grade grade = new Grade();
        grade.setId(7L);
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(gradeRepository.findById(7L)).thenReturn(Optional.of(grade));
        when(pitchRepository.save(any(Pitch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PitchResponse result = pitchService.create(request(1L, 7L, 1));

        assertThat(result.routeId()).isEqualTo(1L);
        assertThat(result.gradeId()).isEqualTo(7L);
        assertThat(result.orderIndex()).isEqualTo(1);
    }

    @Test
    void create_withoutGradeId_leavesGradeNull() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(pitchRepository.save(any(Pitch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PitchResponse result = pitchService.create(request(1L, null, 1));

        assertThat(result.gradeId()).isNull();
        verify(gradeRepository, never()).findById(any());
    }

    @Test
    void create_duplicateOrderIndexOnSameRoute_throws() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(pitchRepository.findByRouteIdAndOrderIndex(1L, 1)).thenReturn(Optional.of(pitch(99L, route(1L), 1)));

        assertThatThrownBy(() -> pitchService.create(request(1L, null, 1)))
                .isInstanceOf(PitchOrderConflictException.class)
                .hasMessageContaining("1");

        verify(pitchRepository, never()).save(any());
    }

    @Test
    void update_keepingOwnOrderIndex_doesNotConflictWithItself() {
        Pitch existing = pitch(1L, route(1L), 1);
        when(pitchRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(pitchRepository.findByRouteIdAndOrderIndex(1L, 1)).thenReturn(Optional.of(existing));
        when(pitchRepository.save(any(Pitch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PitchResponse result = pitchService.update(1L, request(1L, null, 1));

        assertThat(result.orderIndex()).isEqualTo(1);
    }

    @Test
    void update_toOrderIndexHeldByAnotherPitch_throws() {
        Pitch existing = pitch(1L, route(1L), 1);
        Pitch other = pitch(2L, route(1L), 2);
        when(pitchRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(pitchRepository.findByRouteIdAndOrderIndex(1L, 2)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> pitchService.update(1L, request(1L, null, 2)))
                .isInstanceOf(PitchOrderConflictException.class);

        verify(pitchRepository, never()).save(any());
    }

    @Test
    void update_existingPitch_appliesChanges() {
        Pitch existing = pitch(1L, route(1L), 1);
        when(pitchRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route(1L)));
        when(pitchRepository.save(any(Pitch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PitchResponse result = pitchService.update(1L, request(1L, null, 2));

        assertThat(result.orderIndex()).isEqualTo(2);
    }

    @Test
    void update_missingPitch_throws() {
        when(pitchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pitchService.update(99L, request(1L, null, 1)))
                .isInstanceOf(PitchNotFoundException.class);
    }

    @Test
    void delete_existingPitch_deletes() {
        when(pitchRepository.existsById(1L)).thenReturn(true);

        pitchService.delete(1L);

        verify(pitchRepository).deleteById(1L);
    }

    @Test
    void delete_missingPitch_throwsAndDoesNotDelete() {
        when(pitchRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> pitchService.delete(99L)).isInstanceOf(PitchNotFoundException.class);

        verify(pitchRepository, never()).deleteById(99L);
    }
}
