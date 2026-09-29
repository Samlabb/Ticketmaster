package com.ticket.user.application;

import com.ticket.exception.BusinessException;
import com.ticket.security.Role;
import com.ticket.user.domain.AdminAuditLog;
import com.ticket.user.domain.OrderHistoryItem;
import com.ticket.user.domain.UserProfile;
import com.ticket.user.infrastructure.AdminAuditLogRepository;
import com.ticket.user.infrastructure.JwtService;
import com.ticket.user.infrastructure.OrderHistoryRepository;
import com.ticket.user.infrastructure.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyHash;

    public UserService(UserRepository userRepository,
                       OrderHistoryRepository orderHistoryRepository,
                       AdminAuditLogRepository adminAuditLogRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.orderHistoryRepository = orderHistoryRepository;
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        // Для выравнивания времени ответа, когда пользователя с таким email нет
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
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
            throw new BusinessException("User with this email already exists", HttpStatus.CONFLICT, "EMAIL_TAKEN");
        }

        UserProfile user = new UserProfile(normalizedEmail, passwordEncoder.encode(candidatePassword), normalizedName);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserProfile login(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim();
        String rawPassword = password == null ? "" : password;

        UserProfile user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            passwordEncoder.matches(rawPassword, dummyHash);
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw invalidCredentials();
        }

        return user;
    }

    public String generateAccessToken(UserProfile user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
    }

    public String generateRefreshToken(UserProfile user) {
        return jwtService.generateRefreshToken(user.getId(), user.getEmail(), user.getRole());
    }

    public String refreshAccessToken(String refreshToken) {
        if (refreshToken == null || !jwtService.isRefreshTokenValid(refreshToken)) {
            throw new BusinessException("Refresh token is invalid or expired", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }

        UUID userId = UUID.fromString(jwtService.extractUserId(refreshToken));
        UserProfile user = findById(userId);
        return generateAccessToken(user);
    }

    @Transactional(readOnly = true)
    public UserProfile findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
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

    @Transactional
    public UserProfile changeRole(UUID actingAdminId, UUID targetUserId, Role newRole) {
        UserProfile actingAdmin = findById(actingAdminId);
        if (actingAdmin.getRole() != Role.ADMIN) {
            throw new BusinessException("Только администратор может изменять роли", HttpStatus.FORBIDDEN, "ADMIN_REQUIRED");
        }

        UserProfile targetUser = findById(targetUserId);
        Role previousRole = targetUser.getRole();
        targetUser.setRole(newRole);
        userRepository.save(targetUser);

        adminAuditLogRepository.save(AdminAuditLog.roleChanged(
                actingAdmin.getId(), actingAdmin.getEmail(),
                targetUser.getId(), targetUser.getEmail(),
                previousRole, newRole));

        return targetUser;
    }

    private BusinessException invalidCredentials() {
        return new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
    }
}