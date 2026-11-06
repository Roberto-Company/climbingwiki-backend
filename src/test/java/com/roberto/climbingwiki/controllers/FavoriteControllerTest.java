package com.roberto.climbingwiki.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roberto.climbingwiki.dtos.FavoriteResponse;
import com.roberto.climbingwiki.dtos.UpsertResult;
import com.roberto.climbingwiki.exceptions.FavoriteNotFoundException;
import com.roberto.climbingwiki.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.services.FavoriteService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FavoriteController.class)
@AutoConfigureMockMvc(addFilters = false)
class FavoriteControllerTest {

    private static final String VALID_BODY = "{\"userId\":1,\"routeId\":1}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    private FavoriteResponse response(Long userId, Long routeId) {
        return new FavoriteResponse(userId, routeId, null);
    }

    @Test
    void list_withoutFilters_returnsAllFavorites() throws Exception {
        when(favoriteService.findAll()).thenReturn(List.of(response(1L, 1L)));

        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void list_withRouteId_returnsFavoritesForRoute() throws Exception {
        when(favoriteService.findByRoute(1L)).thenReturn(List.of(response(1L, 1L)));

        mockMvc.perform(get("/api/favorites").param("routeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].routeId").value(1));
    }

    @Test
    void list_withUserId_returnsFavoritesByUser() throws Exception {
        when(favoriteService.findByUser(1L)).thenReturn(List.of(response(1L, 7L)));

        mockMvc.perform(get("/api/favorites").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1));
    }

    @Test
    void get_existingFavorite_returnsFavorite() throws Exception {
        when(favoriteService.findOne(1L, 1L)).thenReturn(response(1L, 1L));

        mockMvc.perform(get("/api/favorites/1/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(1));
    }

    @Test
    void get_missingFavorite_returns404() throws Exception {
        when(favoriteService.findOne(1L, 99L)).thenThrow(new FavoriteNotFoundException(1L, 99L));

        mockMvc.perform(get("/api/favorites/1/99")).andExpect(status().isNotFound());
    }

    @Test
    void upsert_newFavorite_returns201() throws Exception {
        when(favoriteService.upsert(any(), any())).thenReturn(new UpsertResult<>(response(1L, 1L), true));

        mockMvc.perform(post("/api/favorites").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void upsert_existingFavorite_returns200() throws Exception {
        when(favoriteService.upsert(any(), any())).thenReturn(new UpsertResult<>(response(1L, 1L), false));

        mockMvc.perform(post("/api/favorites").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void upsert_routeNotFound_returns404() throws Exception {
        when(favoriteService.upsert(any(), any())).thenThrow(new RouteNotFoundException(1L));

        mockMvc.perform(post("/api/favorites").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/favorites/1/1")).andExpect(status().isNoContent());
    }
}
