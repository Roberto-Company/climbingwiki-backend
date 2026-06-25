package com.roberto.climbingwiki.project1.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI climbingWikiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ClimbingWiki API")
                        .description("REST API for routes, crags, sectors, grading, social features and logbooks")
                        .version("v0"));
    }
}
