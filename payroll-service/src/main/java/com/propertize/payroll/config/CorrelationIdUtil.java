package com.propertize.payroll.config;

import org.slf4j.MDC;

import java.util.UUID; /**
 * Utility class for managing correlation IDs for request tracking
 */
public class CorrelationIdUtil {
    
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    
    private CorrelationIdUtil() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Generate a new correlation ID
     */
    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Set correlation ID in MDC (Mapped Diagnostic Context)
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }
    
    /**
     * Get correlation ID from MDC
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }
    
    /**
     * Remove correlation ID from MDC
     */
    public static void clear() {
        MDC.remove(CORRELATION_ID_KEY);
    }
    
    /**
     * Get correlation ID header name
     */
    public static String getHeaderName() {
        return CORRELATION_ID_HEADER;
    }
    
    /**
     * Get correlation ID key for MDC
     */
    public static String getKey() {
        return CORRELATION_ID_KEY;
    }
}
