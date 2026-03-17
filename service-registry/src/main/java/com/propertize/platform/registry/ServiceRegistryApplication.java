package com.propertize.platform.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service Registry Application
 *
 * This is the Eureka Server that acts as a service registry for all microservices
 * in the Propertize platform. All microservices register themselves here and
 * discover other services through this registry.
 *
 * Services registered:
 * - propertize-service (Property Management)
 * - employecraft-service (Employee Management)
 * - api-gateway (API Gateway)
 *
 * @author Propertize Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}
