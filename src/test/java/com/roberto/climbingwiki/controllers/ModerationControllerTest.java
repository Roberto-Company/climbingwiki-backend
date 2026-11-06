package com.roberto.climbingwiki.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.roberto.climbingwiki.dtos.UnverifiedItem;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.services.ModerationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ModerationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ModerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @Test
    void unverified_returnsQueue() throws Exception {
        when(moderationService.unverifiedItems(any())).thenReturn(List.of(
                new UnverifiedItem("sector", 7L, "Gruyère", null, 4L, "El Cejo", 1L, "climber"),
                new UnverifiedItem("pitch", 11L, "Marta", 2, 9L, "Marta", 1L, "climber")));

        mockMvc.perform(get("/api/moderation/unverified"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].kind").value("sector"))
                .andExpect(jsonPath("$[0].parentName").value("El Cejo"))
                .andExpect(jsonPath("$[1].pitchNumber").value(2))
                .andExpect(jsonPath("$[1].createdByUsername").value("climber"));
    }

    @Test
    void unverified_forRegularUser_returns403() throws Exception {
        when(moderationService.unverifiedItems(any())).thenThrow(new ForbiddenException());

        mockMvc.perform(get("/api/moderation/unverified")).andExpect(status().isForbidden());
    }
}
