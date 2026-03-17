package com.propertize.platform.employecraft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Employecraft - Employee Management Microservice
 *
 * Handles HR, Payroll, Attendance, and Benefits management.
 * Integrates with Propertize for organization context and user provisioning.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class EmployecraftApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployecraftApplication.class, args);
    }
}
