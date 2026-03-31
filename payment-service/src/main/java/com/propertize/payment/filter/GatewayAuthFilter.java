package com.propertize.payment.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Authenticates requests based on trusted headers forwarded by the API Gateway.
 * The gateway validates JWTs and propagates user context via X-* headers.
 */
@Slf4j
@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    @Value("${security.gateway.expected-value:api-gateway}")
    private String expectedGatewaySource;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String gatewaySource = request.getHeader("X-Gateway-Source");

        if (!expectedGatewaySource.equals(gatewaySource)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-Roles");

        if (userId == null || userId.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (rolesHeader != null && !rolesHeader.isEmpty()) {
            Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(role -> {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        authorities.add(new SimpleGrantedAuthority(role));
                    });
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null,
                authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Gateway auth: user={}, roles={}", userId, rolesHeader);

        filterChain.doFilter(request, response);
    }
}
