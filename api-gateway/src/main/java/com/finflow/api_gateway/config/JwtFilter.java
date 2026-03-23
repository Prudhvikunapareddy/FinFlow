package com.finflow.api_gateway.config;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
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
        System.out.println("Request Path: " + path);

        // ✅ allow public APIs
        if (path.contains("/auth") || path.contains("/applications/test")) {
            return chain.filter(exchange);
        }

        // 🔐 check token
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("No Token");
        }

        String token = authHeader.substring(7);

        if (!jwtService.validateToken(token)) {
            throw new RuntimeException("Invalid Token");
        }

        return chain.filter(exchange);
    }
}