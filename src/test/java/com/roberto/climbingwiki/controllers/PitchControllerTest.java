package com.roberto.climbingwiki.controllers;

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

import com.roberto.climbingwiki.exceptions.CatalogItemInUseException;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.dtos.PitchResponse;
import com.roberto.climbingwiki.exceptions.PitchNotFoundException;
import com.roberto.climbingwiki.exceptions.PitchOrderConflictException;
import com.roberto.climbingwiki.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.services.PitchService;
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

@WebMvcTest(PitchController.class)
@AutoConfigureMockMvc(addFilters = false)
class PitchControllerTest {

    private static final String VALID_BODY =
            "{\"routeId\":1,\"orderIndex\":1,\"lengthM\":25,\"boltsCount\":8,\"description\":\"crux on top\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PitchService pitchService;

    private PitchResponse response(Long id, Integer orderIndex) {
        return new PitchResponse(id, 1L, null, orderIndex, 25, 8, "crux on top", 3L, true);
    }

    @Test
    void list_withoutRouteId_returnsAllPitches() throws Exception {
        Page<PitchResponse> page = new PageImpl<>(List.of(response(1L, 1), response(2L, 2)));
        when(pitchService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/pitches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        verify(pitchService, never()).findByRouteId(any(), any());
    }

    @Test
    void list_withRouteId_returnsOrderedPitchesForRoute() throws Exception {
        Page<PitchResponse> page = new PageImpl<>(List.of(response(1L, 1), response(2L, 2)));
        when(pitchService.findByRouteId(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/pitches").param("routeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderIndex").value(1))
                .andExpect(jsonPath("$.content[1].orderIndex").value(2));

        verify(pitchService, never()).findAll(any());
    }

    @Test
    void get_existingPitch_returnsPitch() throws Exception {
        when(pitchService.findById(1L)).thenReturn(response(1L, 1));

        mockMvc.perform(get("/api/pitches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(1));
    }

    @Test
    void get_missingPitch_returns404() throws Exception {
        when(pitchService.findById(99L)).thenThrow(new PitchNotFoundException(99L));

        mockMvc.perform(get("/api/pitches/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(pitchService.create(any(), any())).thenReturn(response(5L, 1));

        mockMvc.perform(post("/api/pitches").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/pitches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lengthM\":25}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_routeNotFound_returns404() throws Exception {
        when(pitchService.create(any(), any())).thenThrow(new RouteNotFoundException(1L));

        mockMvc.perform(post("/api/pitches").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_duplicateOrderIndex_returns409() throws Exception {
        when(pitchService.create(any(), any())).thenThrow(new PitchOrderConflictException(1L, 1));

        mockMvc.perform(post("/api/pitches").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void update_existingPitch_returns200() throws Exception {
        when(pitchService.update(eq(1L), any(), any())).thenReturn(response(1L, 1));

        mockMvc.perform(put("/api/pitches/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingPitch_returns404() throws Exception {
        when(pitchService.update(eq(99L), any(), any())).thenThrow(new PitchNotFoundException(99L));

        mockMvc.perform(put("/api/pitches/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingPitch_returns204() throws Exception {
        mockMvc.perform(delete("/api/pitches/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_missingPitch_returns404() throws Exception {
        doThrow(new PitchNotFoundException(99L)).when(pitchService).delete(eq(99L), any());

        mockMvc.perform(delete("/api/pitches/99")).andExpect(status().isNotFound());
    }

    @Test
    void setVerification_validBody_returnsUpdatedItem() throws Exception {
        when(pitchService.setVerified(eq(1L), eq(true), any())).thenReturn(response(1L, 1));

        mockMvc.perform(put("/api/pitches/1/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verified\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void setVerification_missingFlag_returns400() throws Exception {
        mockMvc.perform(put("/api/pitches/1/verification").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setVerification_byRegularUser_returns403() throws Exception {
        when(pitchService.setVerified(eq(1L), eq(true), any())).thenThrow(new ForbiddenException());

        mockMvc.perform(put("/api/pitches/1/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verified\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_itemWithChildData_returns409() throws Exception {
        doThrow(new CatalogItemInUseException()).when(pitchService).delete(eq(1L), any());

        mockMvc.perform(delete("/api/pitches/1")).andExpect(status().isConflict());
    }
}
