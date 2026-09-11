package com.ticket.user.infrastructure;

import com.ticket.user.domain.OrderHistoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderHistoryRepository extends JpaRepository<OrderHistoryItem, UUID> {
    List<OrderHistoryItem> findByUser_IdOrderByPurchasedAtDesc(UUID userId);
    void deleteByUser_IdAndEventIdAndSeatLabel(UUID userId, UUID eventId, String seatLabel);
}
