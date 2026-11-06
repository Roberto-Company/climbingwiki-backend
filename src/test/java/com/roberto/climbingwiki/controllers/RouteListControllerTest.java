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

import com.roberto.climbingwiki.dtos.RouteListResponse;
import com.roberto.climbingwiki.exceptions.RouteListNotFoundException;
import com.roberto.climbingwiki.services.RouteListService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RouteListController.class)
@AutoConfigureMockMvc(addFilters = false)
class RouteListControllerTest {

    private static final String VALID_BODY = "{\"userId\":1,\"name\":\"Classics\",\"description\":\"desc\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RouteListService routeListService;

    private RouteListResponse response(Long id, String name) {
        return new RouteListResponse(id, 1L, name, "desc", true, null);
    }

    @Test
    void list_withoutFilter_returnsAllLists() throws Exception {
        when(routeListService.findAll()).thenReturn(List.of(response(1L, "Classics"), response(2L, "Projects")));

        mockMvc.perform(get("/api/route-lists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void list_withUserId_returnsListsForUser() throws Exception {
        when(routeListService.findByUser(1L)).thenReturn(List.of(response(1L, "Classics")));

        mockMvc.perform(get("/api/route-lists").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1));
    }

    @Test
    void get_existingList_returnsList() throws Exception {
        when(routeListService.findById(1L)).thenReturn(response(1L, "Classics"));

        mockMvc.perform(get("/api/route-lists/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Classics"));
    }

    @Test
    void get_missingList_returns404() throws Exception {
        when(routeListService.findById(99L)).thenThrow(new RouteListNotFoundException(99L));

        mockMvc.perform(get("/api/route-lists/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(routeListService.create(any(), any())).thenReturn(response(5L, "Classics"));

        mockMvc.perform(post("/api/route-lists").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/route-lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_existingList_returns200() throws Exception {
        when(routeListService.update(eq(1L), any(), any())).thenReturn(response(1L, "Classics"));

        mockMvc.perform(put("/api/route-lists/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingList_returns404() throws Exception {
        when(routeListService.update(eq(99L), any(), any())).thenThrow(new RouteListNotFoundException(99L));

        mockMvc.perform(put("/api/route-lists/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingList_returns204() throws Exception {
        mockMvc.perform(delete("/api/route-lists/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_missingList_returns404() throws Exception {
        doThrow(new RouteListNotFoundException(99L)).when(routeListService).delete(eq(99L), any());

        mockMvc.perform(delete("/api/route-lists/99")).andExpect(status().isNotFound());
    }
}
