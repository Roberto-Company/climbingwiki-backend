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

import com.roberto.climbingwiki.project1.dtos.CragResponse;
import com.roberto.climbingwiki.project1.exceptions.CragNotFoundException;
import com.roberto.climbingwiki.project1.services.CragService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CragController.class)
@AutoConfigureMockMvc(addFilters = false)
class CragControllerTest {

    private static final String VALID_BODY = "{\"name\":\"Montserrat\",\"description\":\"desc\","
            + "\"latitude\":41.6,\"longitude\":1.8,\"altitudeM\":700,\"orientation\":\"S\","
            + "\"rockType\":\"limestone\",\"approachDescription\":\"approach\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CragService cragService;

    private CragResponse response(Long id, String name) {
        return new CragResponse(id, null, null, name, "desc", null, null, null, "S", "limestone", "approach", null);
    }

    @Test
    void list_returnsAllCrags() throws Exception {
        Page<CragResponse> page = new PageImpl<>(List.of(response(1L, "Montserrat"), response(2L, "Siurana")));
        when(cragService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/crags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Montserrat"));
    }

    @Test
    void list_withZoneId_filtersByZone() throws Exception {
        Page<CragResponse> page = new PageImpl<>(List.of(response(1L, "Montserrat")));
        when(cragService.findByZoneId(eq(7L), any())).thenReturn(page);

        mockMvc.perform(get("/api/crags").param("zoneId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Montserrat"));
    }

    @Test
    void get_existingCrag_returnsCrag() throws Exception {
        when(cragService.findById(1L)).thenReturn(response(1L, "Montserrat"));

        mockMvc.perform(get("/api/crags/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Montserrat"));
    }

    @Test
    void get_missingCrag_returns404() throws Exception {
        when(cragService.findById(99L)).thenThrow(new CragNotFoundException(99L));

        mockMvc.perform(get("/api/crags/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(cragService.create(any())).thenReturn(response(5L, "Montserrat"));

        mockMvc.perform(post("/api/crags").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Montserrat"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/crags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_existingCrag_returns200() throws Exception {
        when(cragService.update(eq(1L), any())).thenReturn(response(1L, "Montserrat"));

        mockMvc.perform(put("/api/crags/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Montserrat"));
    }

    @Test
    void update_missingCrag_returns404() throws Exception {
        when(cragService.update(eq(99L), any())).thenThrow(new CragNotFoundException(99L));

        mockMvc.perform(put("/api/crags/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingCrag_returns204() throws Exception {
        mockMvc.perform(delete("/api/crags/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_missingCrag_returns404() throws Exception {
        doThrow(new CragNotFoundException(99L)).when(cragService).delete(99L);

        mockMvc.perform(delete("/api/crags/99")).andExpect(status().isNotFound());
    }
}
