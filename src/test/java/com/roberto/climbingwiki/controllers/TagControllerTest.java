package com.roberto.climbingwiki.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roberto.climbingwiki.dtos.TagRequest;
import com.roberto.climbingwiki.dtos.TagResponse;
import com.roberto.climbingwiki.exceptions.TagNotFoundException;
import com.roberto.climbingwiki.services.TagService;
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

@WebMvcTest(TagController.class)
@AutoConfigureMockMvc(addFilters = false)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    @Test
    void list_returnsAllTags() throws Exception {
        Page<TagResponse> page = new PageImpl<>(List.of(new TagResponse(1L, "classic"), new TagResponse(2L, "crimpy")));
        when(tagService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("classic"));
    }

    @Test
    void get_existingTag_returnsTag() throws Exception {
        when(tagService.findById(1L)).thenReturn(new TagResponse(1L, "classic"));

        mockMvc.perform(get("/api/tags/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("classic"));
    }

    @Test
    void get_missingTag_returns404() throws Exception {
        when(tagService.findById(99L)).thenThrow(new TagNotFoundException(Set.of(99L)));

        mockMvc.perform(get("/api/tags/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(tagService.create(eq(new TagRequest("overhang")))).thenReturn(new TagResponse(5L, "overhang"));

        mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"overhang\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("overhang"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_existingTag_returns204() throws Exception {
        mockMvc.perform(delete("/api/tags/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_missingTag_returns404() throws Exception {
        doThrow(new TagNotFoundException(Set.of(99L))).when(tagService).delete(99L);

        mockMvc.perform(delete("/api/tags/99")).andExpect(status().isNotFound());
    }
}
