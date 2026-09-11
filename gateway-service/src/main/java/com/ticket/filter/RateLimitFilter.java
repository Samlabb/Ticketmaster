package com.ticket.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.dto.ErrorResponse;
import com.ticket.service.RateLimitService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = exchange.getRequest().getRemoteAddress() != null ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";

        return rateLimitService.isAllowed(clientIp)
                .flatMap(isAllowed -> {
                    if (isAllowed) {
                        return chain.filter(exchange);
                    }
                    return rejectRequest(exchange, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Слишком много запросов.");
                })
                .onErrorResume(e -> {
                    System.err.println("Redis недоступен: " + e.getMessage());
                    return rejectRequest(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "REDIS_DOWN", "Ошибка сервиса ограничений.");
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private Mono<Void> rejectRequest(ServerWebExchange exchange, HttpStatus status, String errorCode, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse error = ErrorResponse.builder().errorCode(errorCode).message(message).time(LocalDateTime.now()).build();

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(error);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            byte[] fallback = "{\"error\":\"Internal Error\"}".getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }
}