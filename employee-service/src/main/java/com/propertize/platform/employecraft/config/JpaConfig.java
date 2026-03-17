package com.propertize.platform.employecraft.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Configuration with Auditing enabled
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // auditorProvider bean is provided by AuditorAwareImpl @Component
}
