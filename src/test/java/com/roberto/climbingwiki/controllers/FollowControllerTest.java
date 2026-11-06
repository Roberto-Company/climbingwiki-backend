package com.roberto.climbingwiki.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roberto.climbingwiki.dtos.FollowResponse;
import com.roberto.climbingwiki.dtos.UpsertResult;
import com.roberto.climbingwiki.exceptions.FollowNotFoundException;
import com.roberto.climbingwiki.exceptions.InvalidFollowException;
import com.roberto.climbingwiki.services.FollowService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FollowController.class)
@AutoConfigureMockMvc(addFilters = false)
class FollowControllerTest {

    private static final String VALID_BODY = "{\"followerId\":1,\"followedId\":2}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FollowService followService;

    private FollowResponse response(Long followerId, Long followedId) {
        return new FollowResponse(followerId, followedId, null);
    }

    @Test
    void list_withoutFilters_returnsAllFollows() throws Exception {
        when(followService.findAll()).thenReturn(List.of(response(1L, 2L)));

        mockMvc.perform(get("/api/follows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void list_withFollowerId_returnsFollowsByUser() throws Exception {
        when(followService.findByFollower(1L)).thenReturn(List.of(response(1L, 2L)));

        mockMvc.perform(get("/api/follows").param("followerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].followerId").value(1));
    }

    @Test
    void list_withFollowedId_returnsFollowersOfUser() throws Exception {
        when(followService.findByFollowed(2L)).thenReturn(List.of(response(1L, 2L)));

        mockMvc.perform(get("/api/follows").param("followedId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].followedId").value(2));
    }

    @Test
    void get_existingFollow_returnsFollow() throws Exception {
        when(followService.findOne(1L, 2L)).thenReturn(response(1L, 2L));

        mockMvc.perform(get("/api/follows/1/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followedId").value(2));
    }

    @Test
    void get_missingFollow_returns404() throws Exception {
        when(followService.findOne(1L, 99L)).thenThrow(new FollowNotFoundException(1L, 99L));

        mockMvc.perform(get("/api/follows/1/99")).andExpect(status().isNotFound());
    }

    @Test
    void upsert_newFollow_returns201() throws Exception {
        when(followService.upsert(any(), any())).thenReturn(new UpsertResult<>(response(1L, 2L), true));

        mockMvc.perform(post("/api/follows").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void upsert_existingFollow_returns200() throws Exception {
        when(followService.upsert(any(), any())).thenReturn(new UpsertResult<>(response(1L, 2L), false));

        mockMvc.perform(post("/api/follows").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void upsert_selfFollow_returns400() throws Exception {
        when(followService.upsert(any(), any())).thenThrow(new InvalidFollowException());

        mockMvc.perform(post("/api/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followerId\":1,\"followedId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/follows/1/2")).andExpect(status().isNoContent());
    }
}
