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

import com.roberto.climbingwiki.dtos.CommentResponse;
import com.roberto.climbingwiki.exceptions.CommentNotFoundException;
import com.roberto.climbingwiki.exceptions.InvalidCommentParentException;
import com.roberto.climbingwiki.exceptions.InvalidCommentTargetException;
import com.roberto.climbingwiki.services.CommentService;
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

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    private static final String VALID_BODY = "{\"userId\":1,\"routeId\":1,\"body\":\"nice route\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    private CommentResponse response(Long id, Long routeId, Long cragId, String body) {
        return new CommentResponse(id, 1L, routeId, cragId, null, body, null);
    }

    @Test
    void list_withoutFilter_returnsAllComments() throws Exception {
        Page<CommentResponse> page = new PageImpl<>(List.of(response(1L, 1L, null, "nice route")));
        when(commentService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(commentService, never()).findByRoute(any(), any());
        verify(commentService, never()).findByCrag(any(), any());
    }

    @Test
    void list_withRouteId_returnsCommentsForRoute() throws Exception {
        Page<CommentResponse> page = new PageImpl<>(List.of(response(1L, 1L, null, "nice route")));
        when(commentService.findByRoute(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/comments").param("routeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].routeId").value(1));
    }

    @Test
    void list_withCragId_returnsCommentsForCrag() throws Exception {
        Page<CommentResponse> page = new PageImpl<>(List.of(response(1L, null, 1L, "nice crag")));
        when(commentService.findByCrag(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/comments").param("cragId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cragId").value(1));
    }

    @Test
    void get_existingComment_returnsComment() throws Exception {
        when(commentService.findById(1L)).thenReturn(response(1L, 1L, null, "nice route"));

        mockMvc.perform(get("/api/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("nice route"));
    }

    @Test
    void get_missingComment_returns404() throws Exception {
        when(commentService.findById(99L)).thenThrow(new CommentNotFoundException(99L));

        mockMvc.perform(get("/api/comments/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(commentService.create(any(), any())).thenReturn(response(5L, 1L, null, "nice route"));

        mockMvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void create_blankBody_returns400() throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"routeId\":1,\"body\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_neitherRouteNorCrag_returns400() throws Exception {
        when(commentService.create(any(), any())).thenThrow(new InvalidCommentTargetException());

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"body\":\"hi\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_existingComment_returns200() throws Exception {
        when(commentService.update(eq(1L), any(), any())).thenReturn(response(1L, 1L, null, "nice route"));

        mockMvc.perform(put("/api/comments/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingComment_returns404() throws Exception {
        when(commentService.update(eq(99L), any(), any())).thenThrow(new CommentNotFoundException(99L));

        mockMvc.perform(put("/api/comments/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_selfAsParent_returns400() throws Exception {
        when(commentService.update(eq(1L), any(), any())).thenThrow(new InvalidCommentParentException(1L));

        mockMvc.perform(put("/api/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"routeId\":1,\"parentCommentId\":1,\"body\":\"hi\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_existingComment_returns204() throws Exception {
        mockMvc.perform(delete("/api/comments/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_missingComment_returns404() throws Exception {
        doThrow(new CommentNotFoundException(99L)).when(commentService).delete(eq(99L), any());

        mockMvc.perform(delete("/api/comments/99")).andExpect(status().isNotFound());
    }
}
