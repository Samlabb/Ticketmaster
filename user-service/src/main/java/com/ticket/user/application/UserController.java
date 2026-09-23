package com.ticket.user.application;

import com.ticket.user.domain.OrderHistoryItem;
import com.ticket.security.Role;
import com.ticket.user.domain.UserProfile;
import com.ticket.user.infrastructure.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    public record RegisterRequest(String email, String fullName, String password) {}
    public record LoginRequest(String email, String password) {}
    public record RefreshRequest(String refreshToken) {}
    public record OrderRequest(UUID userId, UUID eventId, String eventName, String seatLabel, Double totalPrice) {}
    public record ReturnOrderRequest(UUID userId, UUID eventId, String seatLabel) {}
    public record ChangeRoleRequest(String role) {}

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        UserProfile user = userService.register(request.email(), request.fullName(), request.password());
        String accessToken = userService.generateAccessToken(user);
        String refreshToken = userService.generateRefreshToken(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", user.getId().toString(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "createdAt", user.getCreatedAt().toString(),
                "accessToken", accessToken,
                "refreshToken", refreshToken
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        UserProfile user = userService.login(request.email(), request.password());
        String accessToken = userService.generateAccessToken(user);
        String refreshToken = userService.generateRefreshToken(user);

        return ResponseEntity.ok(Map.of(
                "id", user.getId().toString(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "createdAt", user.getCreatedAt().toString(),
                "accessToken", accessToken,
                "refreshToken", refreshToken
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody RefreshRequest request) {
        String accessToken = userService.refreshAccessToken(request.refreshToken());

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken
        ));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable UUID userId) {
        UserProfile user = userService.findById(userId);

        return ResponseEntity.ok(Map.of(
                "id", user.getId().toString(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "createdAt", user.getCreatedAt().toString()
        ));
    }

    @GetMapping("/{userId}/orders")
    public ResponseEntity<List<Map<String, Object>>> getOrders(@PathVariable UUID userId) {
        List<Map<String, Object>> response = userService.getOrders(userId).stream()
                .map((OrderHistoryItem order) -> {
                    Map<String, Object> dto = new java.util.HashMap<>();
                    dto.put("id", order.getId().toString());
                    dto.put("eventId", order.getEventId().toString());
                    dto.put("eventName", order.getEventName());
                    dto.put("seatLabel", order.getSeatLabel());
                    dto.put("totalPrice", order.getTotalPrice());
                    dto.put("purchasedAt", order.getPurchasedAt().toString());
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> addOrder(@RequestBody OrderRequest request) {
        OrderHistoryItem order = userService.addOrder(
                request.userId(),
                request.eventId(),
                request.eventName(),
                request.seatLabel(),
                request.totalPrice()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", order.getId().toString(),
                "eventId", order.getEventId().toString(),
                "eventName", order.getEventName(),
                "seatLabel", order.getSeatLabel(),
                "totalPrice", order.getTotalPrice(),
                "purchasedAt", order.getPurchasedAt().toString()
        ));
    }

    @PutMapping("/orders/return")
    public ResponseEntity<Map<String, Object>> returnOrder(@RequestBody ReturnOrderRequest request) {
        userService.removeOrder(request.userId(), request.eventId(), request.seatLabel());

        return ResponseEntity.ok(Map.of(
                "status", "RETURNED",
                "userId", request.userId().toString(),
                "eventId", request.eventId().toString(),
                "seatLabel", request.seatLabel()
        ));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<Map<String, Object>> changeRole(
            @PathVariable UUID userId,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody ChangeRoleRequest request
    ) {
        String token = authorizationHeader.replaceFirst("(?i)^Bearer ", "");

        if (!jwtService.isTokenValid(token) || jwtService.extractRole(token) != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Только администратор может изменять роли пользователей"
            ));
        }

        UUID actingAdminId = UUID.fromString(jwtService.extractUserId(token));
        Role newRole;
        try {
            newRole = Role.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Некорректная роль: " + request.role()));
        }

        UserProfile updatedUser = userService.changeRole(actingAdminId, userId, newRole);

        return ResponseEntity.ok(Map.of(
                "id", updatedUser.getId().toString(),
                "email", updatedUser.getEmail(),
                "role", updatedUser.getRole().name()
        ));
    }
}