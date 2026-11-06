package com.roberto.climbingwiki.controllers;

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

import com.roberto.climbingwiki.exceptions.CatalogItemInUseException;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.dtos.SectorResponse;
import com.roberto.climbingwiki.exceptions.CragNotFoundException;
import com.roberto.climbingwiki.exceptions.SectorNotFoundException;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.services.SectorService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SectorController.class)
@AutoConfigureMockMvc(addFilters = false)
class SectorControllerTest {

    private static final String VALID_BODY = "{\"cragId\":1,\"name\":\"Pared de Sant Jeroni\",\"description\":\"desc\","
            + "\"latitude\":41.6,\"longitude\":1.8}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SectorService sectorService;

    private SectorResponse response(Long id, String name) {
        return new SectorResponse(id, 1L, name, "desc", null, null, Set.of(Route.RouteType.SPORT), 3L, true);
    }

    @Test
    void list_returnsAllSectors() throws Exception {
        Page<SectorResponse> page =
                new PageImpl<>(List.of(response(1L, "Pared de Sant Jeroni"), response(2L, "Sector Diedres")));
        when(sectorService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/sectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Pared de Sant Jeroni"));
    }

    @Test
    void list_withCragId_filtersByCrag() throws Exception {
        Page<SectorResponse> page = new PageImpl<>(List.of(response(1L, "Pared de Sant Jeroni")));
        when(sectorService.findByCragId(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/sectors").param("cragId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Pared de Sant Jeroni"));
    }

    @Test
    void get_existingSector_returnsSector() throws Exception {
        when(sectorService.findById(1L)).thenReturn(response(1L, "Pared de Sant Jeroni"));

        mockMvc.perform(get("/api/sectors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cragId").value(1))
                .andExpect(jsonPath("$.routeTypes", hasSize(1)))
                .andExpect(jsonPath("$.routeTypes[0]").value("SPORT"));
    }

    @Test
    void get_missingSector_returns404() throws Exception {
        when(sectorService.findById(99L)).thenThrow(new SectorNotFoundException(99L));

        mockMvc.perform(get("/api/sectors/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(sectorService.create(any(), any())).thenReturn(response(5L, "Pared de Sant Jeroni"));

        mockMvc.perform(post("/api/sectors").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void create_missingCragId_returns400() throws Exception {
        mockMvc.perform(post("/api/sectors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sector\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_cragNotFound_returns404() throws Exception {
        when(sectorService.create(any(), any())).thenThrow(new CragNotFoundException(1L));

        mockMvc.perform(post("/api/sectors").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_existingSector_returns200() throws Exception {
        when(sectorService.update(eq(1L), any(), any())).thenReturn(response(1L, "Pared de Sant Jeroni"));

        mockMvc.perform(put("/api/sectors/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingSector_returns404() throws Exception {
        when(sectorService.update(eq(99L), any(), any())).thenThrow(new SectorNotFoundException(99L));

        mockMvc.perform(put("/api/sectors/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingSector_returns204() throws Exception {
        mockMvc.perform(delete("/api/sectors/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_missingSector_returns404() throws Exception {
        doThrow(new SectorNotFoundException(99L)).when(sectorService).delete(eq(99L), any());

        mockMvc.perform(delete("/api/sectors/99")).andExpect(status().isNotFound());
    }

    @Test
    void setVerification_validBody_returnsUpdatedItem() throws Exception {
        when(sectorService.setVerified(eq(1L), eq(true), any())).thenReturn(response(1L, "Pared de Sant Jeroni"));

        mockMvc.perform(put("/api/sectors/1/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verified\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void setVerification_missingFlag_returns400() throws Exception {
        mockMvc.perform(put("/api/sectors/1/verification").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setVerification_byRegularUser_returns403() throws Exception {
        when(sectorService.setVerified(eq(1L), eq(true), any())).thenThrow(new ForbiddenException());

        mockMvc.perform(put("/api/sectors/1/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verified\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_itemWithChildData_returns409() throws Exception {
        doThrow(new CatalogItemInUseException()).when(sectorService).delete(eq(1L), any());

        mockMvc.perform(delete("/api/sectors/1")).andExpect(status().isConflict());
    }
}
