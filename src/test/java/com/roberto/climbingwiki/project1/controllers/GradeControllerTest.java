package com.roberto.climbingwiki.project1.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roberto.climbingwiki.project1.dtos.GradeResponse;
import com.roberto.climbingwiki.project1.exceptions.GradeConflictException;
import com.roberto.climbingwiki.project1.exceptions.GradeNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.GradingSystemNotFoundException;
import com.roberto.climbingwiki.project1.services.GradeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GradeController.class)
@AutoConfigureMockMvc(addFilters = false)
class GradeControllerTest {

    private static final String VALID_BODY = "{\"gradingSystemId\":1,\"label\":\"6a\",\"difficultyScore\":10}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GradeService gradeService;

    private GradeResponse response(Long id, String label) {
        return new GradeResponse(id, 1L, label, 10);
    }

    @Test
    void list_withoutFilter_returnsAllGrades() throws Exception {
        when(gradeService.findAll()).thenReturn(List.of(response(1L, "6a"), response(2L, "6a+")));

        mockMvc.perform(get("/api/grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(gradeService, never()).findByGradingSystem(any());
    }

    @Test
    void list_withGradingSystemId_returnsGradesForThatSystem() throws Exception {
        when(gradeService.findByGradingSystem(1L)).thenReturn(List.of(response(1L, "6a")));

        mockMvc.perform(get("/api/grades").param("gradingSystemId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("6a"));

        verify(gradeService, never()).findAll();
    }

    @Test
    void get_existingGrade_returnsGrade() throws Exception {
        when(gradeService.findById(1L)).thenReturn(response(1L, "6a"));

        mockMvc.perform(get("/api/grades/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("6a"));
    }

    @Test
    void get_missingGrade_returns404() throws Exception {
        when(gradeService.findById(99L)).thenThrow(new GradeNotFoundException(99L));

        mockMvc.perform(get("/api/grades/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(gradeService.create(any())).thenReturn(response(5L, "6a"));

        mockMvc.perform(post("/api/grades").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void create_blankLabel_returns400() throws Exception {
        mockMvc.perform(post("/api/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradingSystemId\":1,\"label\":\"\",\"difficultyScore\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_gradingSystemNotFound_returns404() throws Exception {
        when(gradeService.create(any())).thenThrow(new GradingSystemNotFoundException(1L));

        mockMvc.perform(post("/api/grades").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_duplicateLabel_returns409() throws Exception {
        when(gradeService.create(any())).thenThrow(new GradeConflictException(1L, "6a"));

        mockMvc.perform(post("/api/grades").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void update_existingGrade_returns200() throws Exception {
        when(gradeService.update(eq(1L), any())).thenReturn(response(1L, "6a"));

        mockMvc.perform(put("/api/grades/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingGrade_returns404() throws Exception {
        when(gradeService.update(eq(99L), any())).thenThrow(new GradeNotFoundException(99L));

        mockMvc.perform(put("/api/grades/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingGrade_returns204() throws Exception {
        mockMvc.perform(delete("/api/grades/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_missingGrade_returns404() throws Exception {
        doThrow(new GradeNotFoundException(99L)).when(gradeService).delete(99L);

        mockMvc.perform(delete("/api/grades/99")).andExpect(status().isNotFound());
    }
}
