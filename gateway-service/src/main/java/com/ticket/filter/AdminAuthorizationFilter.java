package com.ticket.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.dto.ErrorResponse;
import com.ticket.security.JwtValidator;
import com.ticket.security.Role;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class AdminAuthorizationFilter implements GlobalFilter, Ordered {

    private static final String EVENTS_PATH_PREFIX = "/api/events";
    private static final List<HttpMethod> PROTECTED_METHODS = List.of(HttpMethod.POST, HttpMethod.DELETE);

    private final JwtValidator jwtValidator;
    private final ObjectMapper objectMapper;

    public AdminAuthorizationFilter(JwtValidator jwtValidator, ObjectMapper objectMapper) {
        this.jwtValidator = jwtValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        boolean isProtected = path.startsWith(EVENTS_PATH_PREFIX) && PROTECTED_METHODS.contains(method);
        if (!isProtected) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return reject(exchange, "Требуется авторизация администратора");
        }

        String token = authHeader.substring(7);

        if (!jwtValidator.isTokenValid(token) || jwtValidator.extractRole(token) != Role.ADMIN) {
            return reject(exchange, "Только администратор может выполнять это действие");
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse error = ErrorResponse.builder()
                .errorCode("ADMIN_REQUIRED")
                .message(message)
                .time(LocalDateTime.now())
                .build();

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(error);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            byte[] fallback = "{\"error\":\"Forbidden\"}".getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }
}