package com.propertize.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * API Gateway Application
 *
 * This is the single entry point for all client requests to the Propertize platform.
 * It handles:
 * - Request routing to appropriate microservices
 * - JWT token validation
 * - Rate limiting
 * - Circuit breaking
 * - Request/Response logging
 * - CORS handling
 * - RSA key rotation (scheduled)
 *
 * Registered Services:
 * - propertize-service: Property management operations
 * - employecraft-service: Employee management operations
 *
 * @author Propertize Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
