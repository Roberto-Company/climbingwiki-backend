package com.roberto.climbingwiki.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roberto.climbingwiki.dtos.UserResponse;
import com.roberto.climbingwiki.dtos.UserStatsResponse;
import com.roberto.climbingwiki.exceptions.AvatarNotFoundException;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.exceptions.InvalidImageException;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.services.AvatarService;
import com.roberto.climbingwiki.services.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private AvatarService avatarService;

    private UserResponse jane(String avatarUrl) {
        return new UserResponse(7L, "jane", null, "Jane Climber", "Limestone fan", avatarUrl, User.Role.USER, null);
    }

    @Test
    void updateProfile_returnsTheUpdatedUser() throws Exception {
        when(profileService.updateProfile(eq(7L), any(), any())).thenReturn(jane(null));

        mockMvc.perform(put("/api/users/7/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Jane Climber\",\"bio\":\"Limestone fan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jane Climber"))
                .andExpect(jsonPath("$.bio").value("Limestone fan"));
    }

    @Test
    void updateProfile_tooLongBio_returns400() throws Exception {
        String bio = "x".repeat(1001);

        mockMvc.perform(put("/api/users/7/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"" + bio + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.bio").exists());
    }

    @Test
    void updateProfile_forbidden_returns403() throws Exception {
        when(profileService.updateProfile(eq(7L), any(), any())).thenThrow(new ForbiddenException());

        mockMvc.perform(put("/api/users/7/profile").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void stats_returnsTheCounts() throws Exception {
        when(profileService.stats(7L)).thenReturn(new UserStatsResponse(12, 9, 42L, 4, 3, 5));

        mockMvc.perform(get("/api/users/7/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ascents").value(12))
                .andExpect(jsonPath("$.sends").value(9))
                .andExpect(jsonPath("$.hardestSendGradeId").value(42))
                .andExpect(jsonPath("$.followers").value(3));
    }

    @Test
    void avatar_servesTheStoredBytesWithTheirTypeAndCacheHeaders() throws Exception {
        when(avatarService.find(7L)).thenReturn(new AvatarService.AvatarImage("image/png", PNG));

        mockMvc.perform(get("/api/users/7/avatar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("Cache-Control", "max-age=86400, public"));
    }

    @Test
    void avatar_missing_returns404() throws Exception {
        when(avatarService.find(7L)).thenThrow(new AvatarNotFoundException(7L));

        mockMvc.perform(get("/api/users/7/avatar")).andExpect(status().isNotFound());
    }

    @Test
    void uploadAvatar_passesTheFileBytesToTheService() throws Exception {
        when(avatarService.upload(eq(7L), eq(PNG), any())).thenReturn(jane("/api/users/7/avatar?v=1"));
        MockMultipartFile file = new MockMultipartFile("file", "me.png", "image/png", PNG);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/7/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("/api/users/7/avatar?v=1"));
    }

    @Test
    void uploadAvatar_invalidImage_returns400() throws Exception {
        when(avatarService.upload(eq(7L), any(), any())).thenThrow(new InvalidImageException("nope"));
        MockMultipartFile file = new MockMultipartFile("file", "me.svg", "image/svg+xml", "<svg/>".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/7/avatar").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("nope"));
    }

    @Test
    void deleteAvatar_returnsTheUserWithoutPicture() throws Exception {
        when(avatarService.delete(eq(7L), any())).thenReturn(jane(null));

        mockMvc.perform(delete("/api/users/7/avatar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());
    }
}
