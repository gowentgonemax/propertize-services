package com.propertize.payroll.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.propertize.payroll.repository", enableDefaultTransactions = true)
public class JpaConfig {
    // JPA repository configuration with optimized settings
}
