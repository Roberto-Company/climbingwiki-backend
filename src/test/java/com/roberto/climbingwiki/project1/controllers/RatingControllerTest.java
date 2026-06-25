package com.roberto.climbingwiki.project1.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roberto.climbingwiki.project1.dtos.RatingResponse;
import com.roberto.climbingwiki.project1.dtos.UpsertResult;
import com.roberto.climbingwiki.project1.exceptions.InvalidRatingScoreException;
import com.roberto.climbingwiki.project1.exceptions.RatingNotFoundException;
import com.roberto.climbingwiki.project1.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.project1.services.RatingService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RatingController.class)
@AutoConfigureMockMvc(addFilters = false)
class RatingControllerTest {

    private static final String VALID_BODY = "{\"userId\":1,\"routeId\":1,\"score\":4.5}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RatingService ratingService;

    private RatingResponse response(Long userId, Long routeId, BigDecimal score) {
        return new RatingResponse(userId, routeId, score, null);
    }

    @Test
    void list_withoutFilters_returnsAllRatings() throws Exception {
        when(ratingService.findAll()).thenReturn(List.of(response(1L, 1L, BigDecimal.valueOf(4.5))));

        mockMvc.perform(get("/api/ratings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void list_withRouteId_returnsRatingsForRoute() throws Exception {
        when(ratingService.findByRoute(1L)).thenReturn(List.of(response(1L, 1L, BigDecimal.valueOf(4.5))));

        mockMvc.perform(get("/api/ratings").param("routeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].routeId").value(1));
    }

    @Test
    void list_withUserId_returnsRatingsByUser() throws Exception {
        when(ratingService.findByUser(1L)).thenReturn(List.of(response(1L, 7L, BigDecimal.valueOf(3.0))));

        mockMvc.perform(get("/api/ratings").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1));
    }

    @Test
    void get_existingRating_returnsRating() throws Exception {
        when(ratingService.findOne(1L, 1L)).thenReturn(response(1L, 1L, BigDecimal.valueOf(4.5)));

        mockMvc.perform(get("/api/ratings/1/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(4.5));
    }

    @Test
    void get_missingRating_returns404() throws Exception {
        when(ratingService.findOne(1L, 99L)).thenThrow(new RatingNotFoundException(1L, 99L));

        mockMvc.perform(get("/api/ratings/1/99")).andExpect(status().isNotFound());
    }

    @Test
    void upsert_newRating_returns201() throws Exception {
        when(ratingService.upsert(any(), any()))
                .thenReturn(new UpsertResult<>(response(1L, 1L, BigDecimal.valueOf(4.5)), true));

        mockMvc.perform(post("/api/ratings").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void upsert_existingRating_returns200() throws Exception {
        when(ratingService.upsert(any(), any()))
                .thenReturn(new UpsertResult<>(response(1L, 1L, BigDecimal.valueOf(4.5)), false));

        mockMvc.perform(post("/api/ratings").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void upsert_scoreOutOfRange_returns400() throws Exception {
        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"routeId\":1,\"score\":7.5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upsert_invalidScoreStep_returns400() throws Exception {
        when(ratingService.upsert(any(), any())).thenThrow(new InvalidRatingScoreException(BigDecimal.valueOf(3.3)));

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"routeId\":1,\"score\":3.3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upsert_routeNotFound_returns404() throws Exception {
        when(ratingService.upsert(any(), any())).thenThrow(new RouteNotFoundException(1L));

        mockMvc.perform(post("/api/ratings").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/ratings/1/1")).andExpect(status().isNoContent());
    }
}
