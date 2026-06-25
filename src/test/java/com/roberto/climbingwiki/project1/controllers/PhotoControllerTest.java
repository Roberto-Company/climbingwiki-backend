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

import com.roberto.climbingwiki.project1.dtos.PhotoResponse;
import com.roberto.climbingwiki.project1.exceptions.InvalidPhotoTargetException;
import com.roberto.climbingwiki.project1.exceptions.PhotoNotFoundException;
import com.roberto.climbingwiki.project1.services.PhotoService;
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

@WebMvcTest(PhotoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PhotoControllerTest {

    private static final String VALID_BODY = "{\"userId\":1,\"routeId\":1,\"url\":\"a.jpg\",\"caption\":\"nice\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PhotoService photoService;

    private PhotoResponse response(Long id, Long routeId, Long cragId, String url) {
        return new PhotoResponse(id, 1L, routeId, cragId, url, "nice", false, null);
    }

    @Test
    void list_withoutFilter_returnsAllPhotos() throws Exception {
        Page<PhotoResponse> page = new PageImpl<>(List.of(response(1L, 1L, null, "a.jpg")));
        when(photoService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(photoService, never()).findByRoute(any(), any());
        verify(photoService, never()).findByCrag(any(), any());
    }

    @Test
    void list_withRouteId_returnsPhotosForRoute() throws Exception {
        Page<PhotoResponse> page = new PageImpl<>(List.of(response(1L, 1L, null, "a.jpg")));
        when(photoService.findByRoute(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/photos").param("routeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].routeId").value(1));
    }

    @Test
    void list_withCragId_returnsPhotosForCrag() throws Exception {
        Page<PhotoResponse> page = new PageImpl<>(List.of(response(1L, null, 1L, "a.jpg")));
        when(photoService.findByCrag(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/photos").param("cragId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cragId").value(1));
    }

    @Test
    void get_existingPhoto_returnsPhoto() throws Exception {
        when(photoService.findById(1L)).thenReturn(response(1L, 1L, null, "a.jpg"));

        mockMvc.perform(get("/api/photos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("a.jpg"));
    }

    @Test
    void get_missingPhoto_returns404() throws Exception {
        when(photoService.findById(99L)).thenThrow(new PhotoNotFoundException(99L));

        mockMvc.perform(get("/api/photos/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(photoService.create(any(), any())).thenReturn(response(5L, 1L, null, "a.jpg"));

        mockMvc.perform(post("/api/photos").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void create_blankUrl_returns400() throws Exception {
        mockMvc.perform(post("/api/photos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"routeId\":1,\"url\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_neitherRouteNorCrag_returns400() throws Exception {
        when(photoService.create(any(), any())).thenThrow(new InvalidPhotoTargetException());

        mockMvc.perform(post("/api/photos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"url\":\"a.jpg\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_existingPhoto_returns200() throws Exception {
        when(photoService.update(eq(1L), any(), any())).thenReturn(response(1L, 1L, null, "a.jpg"));

        mockMvc.perform(put("/api/photos/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingPhoto_returns404() throws Exception {
        when(photoService.update(eq(99L), any(), any())).thenThrow(new PhotoNotFoundException(99L));

        mockMvc.perform(put("/api/photos/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingPhoto_returns204() throws Exception {
        mockMvc.perform(delete("/api/photos/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_missingPhoto_returns404() throws Exception {
        doThrow(new PhotoNotFoundException(99L)).when(photoService).delete(eq(99L), any());

        mockMvc.perform(delete("/api/photos/99")).andExpect(status().isNotFound());
    }
}
