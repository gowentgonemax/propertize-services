package com.propertize.platform.employecraft.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI employeeServiceOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Employecraft — Employee Service API")
                                                .version("1.0.0")
                                                .description(
                                                                "Manages employees, departments, positions, and compensation for the Propertize platform.")
                                                .contact(new Contact().name("Propertize Team")
                                                                .email("dev@propertize.io"))
                                                .license(new License().name("Proprietary")))
                                .servers(List.of(
                                                new Server().url("http://localhost:8083").description("Direct"),
                                                new Server().url("http://localhost:8080").description("Via Gateway")))
                                .components(new Components()
                                                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")
                                                                .description("JWT token obtained from POST /api/v1/auth/login")))
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
        }
}
