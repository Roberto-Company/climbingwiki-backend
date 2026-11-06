package com.roberto.climbingwiki.project1.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.isNull;
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

import com.roberto.climbingwiki.project1.dtos.ZoneResponse;
import com.roberto.climbingwiki.project1.exceptions.InvalidZoneParentException;
import com.roberto.climbingwiki.project1.exceptions.ZoneNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.services.ZoneService;
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

@WebMvcTest(ZoneController.class)
@AutoConfigureMockMvc(addFilters = false)
class ZoneControllerTest {

    private static final String VALID_BODY = "{\"name\":\"Montserrat\",\"description\":\"desc\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZoneService zoneService;

    private ZoneResponse response(Long id, String name) {
        return new ZoneResponse(id, null, name, "desc", Set.of(Route.RouteType.SPORT));
    }

    @Test
    void list_returnsAllZones() throws Exception {
        Page<ZoneResponse> page = new PageImpl<>(List.of(response(1L, "Montserrat"), response(2L, "Siurana")));
        when(zoneService.search(isNull(), eq(false), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Montserrat"));
    }

    @Test
    void list_withParentZoneId_filtersByParent() throws Exception {
        Page<ZoneResponse> page = new PageImpl<>(List.of(response(2L, "Sant Jeroni")));
        when(zoneService.search(eq(1L), eq(false), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/zones").param("parentZoneId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Sant Jeroni"));
    }

    @Test
    void list_withRootOnly_returnsRootZones() throws Exception {
        Page<ZoneResponse> page = new PageImpl<>(List.of(response(1L, "Montserrat")));
        when(zoneService.search(isNull(), eq(true), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/zones").param("rootOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Montserrat"));
    }

    @Test
    void list_withRootOnlyAndName_combinesBothFilters() throws Exception {
        when(zoneService.search(isNull(), eq(true), eq("Mur"), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/zones").param("rootOnly", "true").param("name", "Mur"))
                .andExpect(status().isOk());

        verify(zoneService).search(isNull(), eq(true), eq("Mur"), any());
    }

    @Test
    void get_existingZone_returnsZone() throws Exception {
        when(zoneService.findById(1L)).thenReturn(response(1L, "Montserrat"));

        mockMvc.perform(get("/api/zones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Montserrat"));
    }

    @Test
    void get_missingZone_returns404() throws Exception {
        when(zoneService.findById(99L)).thenThrow(new ZoneNotFoundException(99L));

        mockMvc.perform(get("/api/zones/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(zoneService.create(any())).thenReturn(response(5L, "Montserrat"));

        mockMvc.perform(post("/api/zones").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Montserrat"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_existingZone_returns200() throws Exception {
        when(zoneService.update(eq(1L), any())).thenReturn(response(1L, "Montserrat"));

        mockMvc.perform(put("/api/zones/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Montserrat"));
    }

    @Test
    void update_missingZone_returns404() throws Exception {
        when(zoneService.update(eq(99L), any())).thenThrow(new ZoneNotFoundException(99L));

        mockMvc.perform(put("/api/zones/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_selfAsParent_returns400() throws Exception {
        when(zoneService.update(eq(1L), any())).thenThrow(new InvalidZoneParentException(1L));

        mockMvc.perform(put("/api/zones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentZoneId\":1,\"name\":\"Montserrat\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_existingZone_returns204() throws Exception {
        mockMvc.perform(delete("/api/zones/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_missingZone_returns404() throws Exception {
        doThrow(new ZoneNotFoundException(99L)).when(zoneService).delete(99L);

        mockMvc.perform(delete("/api/zones/99")).andExpect(status().isNotFound());
    }
}
