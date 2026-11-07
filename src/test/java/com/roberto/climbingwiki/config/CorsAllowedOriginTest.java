package com.roberto.climbingwiki.config;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "climbingwiki.security.cors-allowed-origins=" + CorsTestSupport.SITE)
class CorsAllowedOriginTest extends CorsTestSupport {

    @Test
    void preflight_fromAllowedOrigin_isAccepted() throws Exception {
        preflightFrom(SITE)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SITE));
    }

    @Test
    void preflight_fromOtherOrigin_isRejected() throws Exception {
        preflightFrom("https://evil.example").andExpect(status().isForbidden());
    }
}
