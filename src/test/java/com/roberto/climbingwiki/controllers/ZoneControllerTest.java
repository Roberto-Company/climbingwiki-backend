package com.roberto.climbingwiki.controllers;

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

import com.roberto.climbingwiki.exceptions.CatalogItemInUseException;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.dtos.ZoneResponse;
import com.roberto.climbingwiki.exceptions.InvalidZoneParentException;
import com.roberto.climbingwiki.exceptions.ZoneNotFoundException;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.services.ZoneService;
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
        return new ZoneResponse(id, null, name, "desc", Set.of(Route.RouteType.SPORT), 3L, true);
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
        when(zoneService.create(any(), any())).thenReturn(response(5L, "Montserrat"));

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
        when(zoneService.update(eq(1L), any(), any())).thenReturn(response(1L, "Montserrat"));

        mockMvc.perform(put("/api/zones/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Montserrat"));
    }

    @Test
    void update_missingZone_returns404() throws Exception {
        when(zoneService.update(eq(99L), any(), any())).thenThrow(new ZoneNotFoundException(99L));

        mockMvc.perform(put("/api/zones/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_selfAsParent_returns400() throws Exception {
        when(zoneService.update(eq(1L), any(), any())).thenThrow(new InvalidZoneParentException(1L));

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
        doThrow(new ZoneNotFoundException(99L)).when(zoneService).delete(eq(99L), any());

        mockMvc.perform(delete("/api/zones/99")).andExpect(status().isNotFound());
    }

    @Test
    void setVerification_validBody_returnsUpdatedItem() throws Exception {
        when(zoneService.setVerified(eq(1L), eq(true), any())).thenReturn(response(1L, "Montserrat"));

        mockMvc.perform(put("/api/zones/1/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verified\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void setVerification_missingFlag_returns400() throws Exception {
        mockMvc.perform(put("/api/zones/1/verification").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setVerification_byRegularUser_returns403() throws Exception {
        when(zoneService.setVerified(eq(1L), eq(true), any())).thenThrow(new ForbiddenException());

        mockMvc.perform(put("/api/zones/1/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verified\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_itemWithChildData_returns409() throws Exception {
        doThrow(new CatalogItemInUseException()).when(zoneService).delete(eq(1L), any());

        mockMvc.perform(delete("/api/zones/1")).andExpect(status().isConflict());
    }
}
