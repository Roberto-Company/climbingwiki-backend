package com.roberto.climbingwiki.project1.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roberto.climbingwiki.project1.dtos.GradingSystemResponse;
import com.roberto.climbingwiki.project1.exceptions.GradingSystemConflictException;
import com.roberto.climbingwiki.project1.exceptions.GradingSystemNotFoundException;
import com.roberto.climbingwiki.project1.model.GradingTypes;
import com.roberto.climbingwiki.project1.services.GradingSystemService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GradingSystemController.class)
@AutoConfigureMockMvc(addFilters = false)
class GradingSystemControllerTest {

    private static final String VALID_BODY = "{\"name\":\"French\",\"category\":\"ROPED\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GradingSystemService gradingSystemService;

    private GradingSystemResponse response(Long id, String name) {
        return new GradingSystemResponse(id, name, GradingTypes.Category.ROPED);
    }

    @Test
    void list_returnsAllSystems() throws Exception {
        when(gradingSystemService.findAll()).thenReturn(List.of(response(1L, "French"), response(2L, "YDS")));

        mockMvc.perform(get("/api/grading-systems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void get_existingSystem_returnsSystem() throws Exception {
        when(gradingSystemService.findById(1L)).thenReturn(response(1L, "French"));

        mockMvc.perform(get("/api/grading-systems/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("French"));
    }

    @Test
    void get_missingSystem_returns404() throws Exception {
        when(gradingSystemService.findById(99L)).thenThrow(new GradingSystemNotFoundException(99L));

        mockMvc.perform(get("/api/grading-systems/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(gradingSystemService.create(any())).thenReturn(response(5L, "French"));

        mockMvc.perform(post("/api/grading-systems").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/grading-systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"category\":\"ROPED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        when(gradingSystemService.create(any())).thenThrow(new GradingSystemConflictException("French"));

        mockMvc.perform(post("/api/grading-systems").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void update_existingSystem_returns200() throws Exception {
        when(gradingSystemService.update(eq(1L), any())).thenReturn(response(1L, "French"));

        mockMvc.perform(put("/api/grading-systems/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingSystem_returns404() throws Exception {
        when(gradingSystemService.update(eq(99L), any())).thenThrow(new GradingSystemNotFoundException(99L));

        mockMvc.perform(put("/api/grading-systems/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingSystem_returns204() throws Exception {
        mockMvc.perform(delete("/api/grading-systems/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_missingSystem_returns404() throws Exception {
        doThrow(new GradingSystemNotFoundException(99L)).when(gradingSystemService).delete(99L);

        mockMvc.perform(delete("/api/grading-systems/99")).andExpect(status().isNotFound());
    }
}
