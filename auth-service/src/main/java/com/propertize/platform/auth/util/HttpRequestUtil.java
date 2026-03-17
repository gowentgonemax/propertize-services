package com.propertize.platform.auth.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

/**
 * Utility class for common HTTP request operations
 */
@UtilityClass
public class HttpRequestUtil {

    /**
     * Extract client IP address from HTTP request
     * Handles proxies, load balancers, and various forwarding headers
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (isValidIp(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (isValidIp(xRealIp)) {
            return xRealIp;
        }

        String[] otherHeaders = {
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : otherHeaders) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                return ip;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "UNKNOWN";
    }

    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank() &&
                !"unknown".equalsIgnoreCase(ip) &&
                !"UNKNOWN".equals(ip);
    }
}
