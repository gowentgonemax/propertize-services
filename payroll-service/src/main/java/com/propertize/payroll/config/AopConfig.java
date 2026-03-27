package com.propertize.payroll.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.UUID;

/**
 * Configuration for enabling AOP (Aspect-Oriented Programming)
 */
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {
    // AOP configuration for logging aspects
}


