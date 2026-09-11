package com.ticket.user.application;

import com.ticket.user.domain.OrderHistoryItem;
import com.ticket.user.domain.UserProfile;
import com.ticket.user.infrastructure.JwtService;
import com.ticket.user.infrastructure.OrderHistoryRepository;
import com.ticket.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderHistoryRepository orderHistoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateOrderForExistingUser() {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UserProfile user = new UserProfile("user@example.com", "hash", "User");

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(orderHistoryRepository.save(any(OrderHistoryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.addOrder(userId, eventId, "Concert", "A1", 5000.0);

        verify(orderHistoryRepository).save(any(OrderHistoryItem.class));
    }

    @Test
    void shouldRemoveOrderDuringReturnCompensation() {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        userService.removeOrder(userId, eventId, "A1");

        verify(orderHistoryRepository).deleteByUser_IdAndEventIdAndSeatLabel(userId, eventId, "A1");
    }
}