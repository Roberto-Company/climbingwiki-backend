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

import com.roberto.climbingwiki.project1.dtos.LogbookEntryResponse;
import com.roberto.climbingwiki.project1.exceptions.LogbookEntryNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.LogbookEntry;
import com.roberto.climbingwiki.project1.services.LogbookEntryService;
import java.time.LocalDate;
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

@WebMvcTest(LogbookEntryController.class)
@AutoConfigureMockMvc(addFilters = false)
class LogbookEntryControllerTest {

    private static final String VALID_BODY =
            "{\"userId\":1,\"routeId\":1,\"attemptDate\":\"2026-01-01\",\"result\":\"REDPOINT\",\"notes\":\"clean\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogbookEntryService logbookEntryService;

    private LogbookEntryResponse response(Long id, Long userId, Long routeId, LogbookEntry.Result result) {
        return new LogbookEntryResponse(id, userId, routeId, LocalDate.of(2026, 1, 1), result, "clean", null);
    }

    @Test
    void list_withoutFilter_returnsAllEntries() throws Exception {
        Page<LogbookEntryResponse> page = new PageImpl<>(List.of(response(1L, 1L, 1L, LogbookEntry.Result.REDPOINT)));
        when(logbookEntryService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/logbook-entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(logbookEntryService, never()).findByUser(any(), any());
        verify(logbookEntryService, never()).findByRoute(any(), any());
    }

    @Test
    void list_withUserId_returnsEntriesForUser() throws Exception {
        Page<LogbookEntryResponse> page = new PageImpl<>(List.of(response(1L, 1L, 1L, LogbookEntry.Result.REDPOINT)));
        when(logbookEntryService.findByUser(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/logbook-entries").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(1));
    }

    @Test
    void list_withRouteId_returnsEntriesForRoute() throws Exception {
        Page<LogbookEntryResponse> page = new PageImpl<>(List.of(response(1L, 1L, 1L, LogbookEntry.Result.FLASH)));
        when(logbookEntryService.findByRoute(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/logbook-entries").param("routeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].routeId").value(1));
    }

    @Test
    void get_existingEntry_returnsEntry() throws Exception {
        when(logbookEntryService.findById(1L)).thenReturn(response(1L, 1L, 1L, LogbookEntry.Result.ONSIGHT));

        mockMvc.perform(get("/api/logbook-entries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("ONSIGHT"));
    }

    @Test
    void get_missingEntry_returns404() throws Exception {
        when(logbookEntryService.findById(99L)).thenThrow(new LogbookEntryNotFoundException(99L));

        mockMvc.perform(get("/api/logbook-entries/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(logbookEntryService.create(any(), any())).thenReturn(response(5L, 1L, 1L, LogbookEntry.Result.REDPOINT));

        mockMvc.perform(post("/api/logbook-entries").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/logbook-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_existingEntry_returns200() throws Exception {
        when(logbookEntryService.update(eq(1L), any(), any()))
                .thenReturn(response(1L, 1L, 1L, LogbookEntry.Result.REDPOINT));

        mockMvc.perform(put("/api/logbook-entries/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingEntry_returns404() throws Exception {
        when(logbookEntryService.update(eq(99L), any(), any())).thenThrow(new LogbookEntryNotFoundException(99L));

        mockMvc.perform(put("/api/logbook-entries/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingEntry_returns204() throws Exception {
        mockMvc.perform(delete("/api/logbook-entries/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_missingEntry_returns404() throws Exception {
        doThrow(new LogbookEntryNotFoundException(99L)).when(logbookEntryService).delete(eq(99L), any());

        mockMvc.perform(delete("/api/logbook-entries/99")).andExpect(status().isNotFound());
    }
}
