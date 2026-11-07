package com.roberto.climbingwiki.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

import com.roberto.climbingwiki.controllers.ZoneController;
import com.roberto.climbingwiki.security.JwtService;
import com.roberto.climbingwiki.services.ZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Runs requests through the real security chain ({@link SecurityConfig}) to check CORS. */
@WebMvcTest(ZoneController.class)
@Import(SecurityConfig.class)
abstract class CorsTestSupport {

    static final String SITE = "https://climbingwiki.example";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ZoneService zoneService;

    @MockitoBean
    JwtService jwtService;

    ResultActions preflightFrom(String origin) throws Exception {
        return mockMvc.perform(options("/api/zones")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"));
    }
}
