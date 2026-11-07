package com.roberto.climbingwiki.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;

/** The site calls the API server-side, so by default no browser origin is allowed. */
class CorsDefaultTest extends CorsTestSupport {

    @Test
    void preflight_fromAnyOrigin_isRejected() throws Exception {
        preflightFrom(SITE)
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void serverSideRequest_withoutOriginHeader_stillWorks() throws Exception {
        when(zoneService.search(any(), anyBoolean(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/zones")).andExpect(status().isOk());
    }
}
