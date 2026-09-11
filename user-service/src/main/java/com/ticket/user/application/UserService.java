package com.ticket.user.application;

import com.ticket.user.domain.OrderHistoryItem;
import com.ticket.user.domain.UserProfile;
import com.ticket.user.infrastructure.JwtService;
import com.ticket.user.infrastructure.OrderHistoryRepository;
import com.ticket.user.infrastructure.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       OrderHistoryRepository orderHistoryRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.orderHistoryRepository = orderHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserProfile register(String email, String fullName, String password) {
        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedName = fullName == null ? "" : fullName.trim();
        String candidatePassword = password == null ? "" : password;

        if (normalizedEmail.isBlank() || normalizedName.isBlank() || candidatePassword.isBlank()) {
            throw new IllegalArgumentException("Email, full name and password are required");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        UserProfile user = new UserProfile(normalizedEmail, passwordEncoder.encode(candidatePassword), normalizedName);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserProfile login(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim();
        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        UserProfile user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        return user;
    }

    public String generateAccessToken(UserProfile user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail());
    }

    public String generateRefreshToken(UserProfile user) {
        return jwtService.generateRefreshToken(user.getId(), user.getEmail());
    }

    public String refreshAccessToken(String refreshToken) {
        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Refresh token is invalid or expired");
        }

        UUID userId = UUID.fromString(jwtService.extractUserId(refreshToken));
        UserProfile user = findById(userId);
        return generateAccessToken(user);
    }

    @Transactional(readOnly = true)
    public UserProfile findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryItem> getOrders(UUID userId) {
        return orderHistoryRepository.findByUser_IdOrderByPurchasedAtDesc(userId);
    }

    @Transactional
    public OrderHistoryItem addOrder(UUID userId, UUID eventId, String eventName, String seatLabel, Double totalPrice) {
        UserProfile user = findById(userId);
        OrderHistoryItem order = new OrderHistoryItem(user, eventId, eventName, seatLabel, totalPrice);
        return orderHistoryRepository.save(order);
    }

    @Transactional
    public void removeOrder(UUID userId, UUID eventId, String seatLabel) {
        orderHistoryRepository.deleteByUser_IdAndEventIdAndSeatLabel(userId, eventId, seatLabel);
    }
}
