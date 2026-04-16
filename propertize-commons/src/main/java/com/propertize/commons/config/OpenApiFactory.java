package com.propertize.commons.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

/**
 * Factory for constructing a consistent OpenAPI definition across all services.
 * <p>
 * Each service instantiates a {@code @Bean} that delegates to this factory
 * so all Swagger UIs share the same JWT security scheme and branding.
 *
 * <pre>{@code
 * @Bean
 * public OpenAPI payrollOpenAPI() {
 *     return OpenApiFactory.create(
 *         "Payroll Service API", "1.0.0",
 *         "Payroll processing endpoints",
 *         8085, 8080);
 * }
 * }</pre>
 */
public final class OpenApiFactory {

    private OpenApiFactory() {
        // utility class
    }

    /**
     * Build an OpenAPI definition with JWT bearer security.
     *
     * @param title       API title (e.g. "Payroll Service API")
     * @param version     API version (e.g. "1.0.0")
     * @param description short description
     * @param directPort  the service's own port
     * @param gatewayPort the API gateway port (usually 8080)
     * @return fully configured {@link OpenAPI} instance
     */
    public static OpenAPI create(String title, String version, String description,
                                 int directPort, int gatewayPort) {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .version(version)
                        .description(description)
                        .contact(new Contact().name("Propertize Team").email("dev@propertize.io"))
                        .license(new License().name("Proprietary")))
                .servers(List.of(
                        new Server().url("http://localhost:" + directPort).description("Direct"),
                        new Server().url("http://localhost:" + gatewayPort).description("Via Gateway")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from POST /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}

