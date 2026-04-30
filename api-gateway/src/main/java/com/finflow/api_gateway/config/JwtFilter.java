package com.finflow.api_gateway.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.finflow.api_gateway.service.JwtService;

import reactor.core.publisher.Mono;

@Component
public class JwtFilter implements GlobalFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        if (!jwtService.validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String role = jwtService.extractRole(token);
        if (isAdminPath(path) && (role == null || !"ADMIN".equals(role.trim().toUpperCase(Locale.ROOT)))) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header("X-User-Email", jwtService.extractEmail(token))
                .header("X-User-Role", role == null ? "" : role)
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/gateway/auth/login")
                || path.startsWith("/gateway/auth/signup")
                || path.startsWith("/auth/login")
                || path.startsWith("/auth/signup")
                || path.endsWith("/test")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/webjars")
                || "/swagger-ui.html".equals(path);
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/gateway/admin") || path.startsWith("/admin");
    }
}
