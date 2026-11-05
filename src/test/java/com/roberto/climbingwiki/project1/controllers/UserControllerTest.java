package com.roberto.climbingwiki.project1.controllers;

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

import com.roberto.climbingwiki.project1.dtos.UserResponse;
import com.roberto.climbingwiki.project1.exceptions.ForbiddenException;
import com.roberto.climbingwiki.project1.exceptions.UserConflictException;
import com.roberto.climbingwiki.project1.exceptions.UserNotFoundException;
import com.roberto.climbingwiki.project1.model.entities.User;
import com.roberto.climbingwiki.project1.services.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    private static final String VALID_BODY = "{\"username\":\"jane\",\"email\":\"jane@example.com\","
            + "\"password\":\"supersecret\",\"fullName\":\"Jane Climber\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UserResponse response(Long id, String username) {
        return new UserResponse(id, username, "jane@example.com", "Jane Climber", null, null, User.Role.USER, null);
    }

    @Test
    void list_returnsAllUsers() throws Exception {
        when(userService.findAll(any())).thenReturn(List.of(response(1L, "jane"), response(2L, "bob")));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("jane"));
    }

    @Test
    void list_neverExposesPasswordHash() throws Exception {
        when(userService.findAll(any())).thenReturn(List.of(response(1L, "jane")));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void list_hiddenEmail_isNotExposed() throws Exception {
        UserResponse withoutEmail =
                new UserResponse(1L, "jane", null, "Jane Climber", null, null, User.Role.USER, null);
        when(userService.findAll(any())).thenReturn(List.of(withoutEmail));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    void get_existingUser_returnsUser() throws Exception {
        when(userService.findById(eq(1L), any())).thenReturn(response(1L, "jane"));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jane"));
    }

    @Test
    void get_missingUser_returns404() throws Exception {
        when(userService.findById(eq(99L), any())).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(userService.create(any(), any())).thenReturn(response(5L, "jane"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void create_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"jane\",\"email\":\"jane@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"jane\",\"email\":\"not-an-email\",\"password\":\"supersecret\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_duplicateUsername_returns409() throws Exception {
        when(userService.create(any(), any())).thenThrow(new UserConflictException("username", "jane"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void update_existingUser_returns200() throws Exception {
        when(userService.update(eq(1L), any(), any())).thenReturn(response(1L, "jane"));

        mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingUser_returns404() throws Exception {
        when(userService.update(eq(99L), any(), any())).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(put("/api/users/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_notOwnerOrAdmin_returns403() throws Exception {
        when(userService.update(eq(1L), any(), any())).thenThrow(new ForbiddenException());

        mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_existingUser_returns204() throws Exception {
        mockMvc.perform(delete("/api/users/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_missingUser_returns404() throws Exception {
        doThrow(new UserNotFoundException(99L)).when(userService).delete(eq(99L), any());

        mockMvc.perform(delete("/api/users/99")).andExpect(status().isNotFound());
    }

    @Test
    void delete_notOwnerOrAdmin_returns403() throws Exception {
        doThrow(new ForbiddenException()).when(userService).delete(eq(1L), any());

        mockMvc.perform(delete("/api/users/1")).andExpect(status().isForbidden());
    }
}
