package com.roberto.climbingwiki.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roberto.climbingwiki.dtos.ListRouteResponse;
import com.roberto.climbingwiki.dtos.UpsertResult;
import com.roberto.climbingwiki.exceptions.ListRouteNotFoundException;
import com.roberto.climbingwiki.exceptions.RouteListNotFoundException;
import com.roberto.climbingwiki.services.ListRouteService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ListRouteController.class)
@AutoConfigureMockMvc(addFilters = false)
class ListRouteControllerTest {

    private static final String VALID_BODY = "{\"listId\":1,\"routeId\":10}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListRouteService listRouteService;

    private ListRouteResponse response(Long listId, Long routeId) {
        return new ListRouteResponse(listId, routeId, null);
    }

    @Test
    void list_withoutFilters_returnsAllListRoutes() throws Exception {
        when(listRouteService.findAll()).thenReturn(List.of(response(1L, 10L)));

        mockMvc.perform(get("/api/list-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void list_withListId_returnsRoutesInList() throws Exception {
        when(listRouteService.findByList(1L)).thenReturn(List.of(response(1L, 10L)));

        mockMvc.perform(get("/api/list-routes").param("listId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].listId").value(1));
    }

    @Test
    void list_withRouteId_returnsListsContainingRoute() throws Exception {
        when(listRouteService.findByRoute(10L)).thenReturn(List.of(response(1L, 10L)));

        mockMvc.perform(get("/api/list-routes").param("routeId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].routeId").value(10));
    }

    @Test
    void get_existingListRoute_returnsListRoute() throws Exception {
        when(listRouteService.findOne(1L, 10L)).thenReturn(response(1L, 10L));

        mockMvc.perform(get("/api/list-routes/1/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(10));
    }

    @Test
    void get_missingListRoute_returns404() throws Exception {
        when(listRouteService.findOne(1L, 99L)).thenThrow(new ListRouteNotFoundException(1L, 99L));

        mockMvc.perform(get("/api/list-routes/1/99")).andExpect(status().isNotFound());
    }

    @Test
    void upsert_newListRoute_returns201() throws Exception {
        when(listRouteService.upsert(any(), any())).thenReturn(new UpsertResult<>(response(1L, 10L), true));

        mockMvc.perform(post("/api/list-routes").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void upsert_existingListRoute_returns200() throws Exception {
        when(listRouteService.upsert(any(), any())).thenReturn(new UpsertResult<>(response(1L, 10L), false));

        mockMvc.perform(post("/api/list-routes").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void upsert_listNotFound_returns404() throws Exception {
        when(listRouteService.upsert(any(), any())).thenThrow(new RouteListNotFoundException(1L));

        mockMvc.perform(post("/api/list-routes").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/list-routes/1/10")).andExpect(status().isNoContent());
    }
}
