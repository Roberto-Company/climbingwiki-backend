package com.roberto.climbingwiki.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.isNull;
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

import com.roberto.climbingwiki.exceptions.CatalogItemInUseException;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.dtos.RouteResponse;
import com.roberto.climbingwiki.exceptions.RouteNotFoundException;
import com.roberto.climbingwiki.exceptions.SectorNotFoundException;
import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.services.RouteService;
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

@WebMvcTest(RouteController.class)
@AutoConfigureMockMvc(addFilters = false)
class RouteControllerTest {

    private static final String VALID_BODY = "{\"sectorId\":1,\"name\":\"La Rambla\",\"routeType\":\"SPORT\","
            + "\"description\":\"desc\",\"equipmentNotes\":\"notes\",\"firstAscentBy\":\"John Doe\","
            + "\"tagIds\":[1]}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RouteService routeService;

    private RouteResponse response(Long id, String name) {
        return new RouteResponse(
                id, 1L, null, name, Route.RouteType.SPORT, "desc", "notes", "John Doe", null, null, null, null, null,
                Set.of("classic"), 12L, 45, true);
    }

    @Test
    void list_returnsAllRoutes() throws Exception {
        Page<RouteResponse> page = new PageImpl<>(List.of(response(1L, "La Rambla"), response(2L, "Bla")));
        when(routeService.search(isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("La Rambla"))
                .andExpect(jsonPath("$.content[0].hardestGradeId").value(12))
                .andExpect(jsonPath("$.content[0].totalLengthM").value(45))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void list_withSectorId_filtersBySector() throws Exception {
        Page<RouteResponse> page = new PageImpl<>(List.of(response(1L, "La Rambla")));
        when(routeService.search(eq(1L), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/routes").param("sectorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("La Rambla"));
    }

    @Test
    void list_withSectorIdAndName_combinesBothFilters() throws Exception {
        when(routeService.search(eq(6L), eq("Marta"), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/routes").param("sectorId", "6").param("name", "Marta"))
                .andExpect(status().isOk());

        verify(routeService).search(eq(6L), eq("Marta"), any());
    }

    @Test
    void get_existingRoute_returnsRoute() throws Exception {
        when(routeService.findById(1L)).thenReturn(response(1L, "La Rambla"));

        mockMvc.perform(get("/api/routes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeType").value("SPORT"));
    }

    @Test
    void get_missingRoute_returns404() throws Exception {
        when(routeService.findById(99L)).thenThrow(new RouteNotFoundException(99L));

        mockMvc.perform(get("/api/routes/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(routeService.create(any(), any())).thenReturn(response(5L, "La Rambla"));

        mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_sectorNotFound_returns404() throws Exception {
        when(routeService.create(any(), any())).thenThrow(new SectorNotFoundException(1L));

        mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_existingRoute_returns200() throws Exception {
        when(routeService.update(eq(1L), any(), any())).thenReturn(response(1L, "La Rambla"));

        mockMvc.perform(put("/api/routes/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingRoute_returns404() throws Exception {
        when(routeService.update(eq(99L), any(), any())).thenThrow(new RouteNotFoundException(99L));

        mockMvc.perform(put("/api/routes/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingRoute_returns204() throws Exception {
        mockMvc.perform(delete("/api/routes/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_missingRoute_returns404() throws Exception {
        doThrow(new RouteNotFoundException(99L)).when(routeService).delete(eq(99L), any());

        mockMvc.perform(delete("/api/routes/99")).andExpect(status().isNotFound());
    }

    @Test
    void setVerification_validBody_returnsUpdatedItem() throws Exception {
        when(routeService.setVerified(eq(1L), eq(true), any())).thenReturn(response(1L, "La Rambla"));

        mockMvc.perform(put("/api/routes/1/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verified\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void setVerification_missingFlag_returns400() throws Exception {
        mockMvc.perform(put("/api/routes/1/verification").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setVerification_byRegularUser_returns403() throws Exception {
        when(routeService.setVerified(eq(1L), eq(true), any())).thenThrow(new ForbiddenException());

        mockMvc.perform(put("/api/routes/1/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verified\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_itemWithChildData_returns409() throws Exception {
        doThrow(new CatalogItemInUseException()).when(routeService).delete(eq(1L), any());

        mockMvc.perform(delete("/api/routes/1")).andExpect(status().isConflict());
    }
}
