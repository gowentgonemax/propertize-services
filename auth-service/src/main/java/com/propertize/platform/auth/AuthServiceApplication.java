package com.propertize.platform.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Auth Service Application
 * 
 * Centralized Authentication and Authorization Service for Propertize
 * Microservices Platform
 * 
 * Features:
 * - JWT Token Generation (Access & Refresh Tokens)
 * - RSA Key-based Token Signing
 * - Token Blacklist Management
 * - User Management
 * - Role-Based Access Control (RBAC)
 * - Service-to-Service Authentication
 * 
 * @author Propertize Platform Team
 * @version 2.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
