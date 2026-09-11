package com.ticket.user.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_order_history")
public class OrderHistoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String eventName;

    @Column(nullable = false)
    private String seatLabel;

    @Column(nullable = false)
    private Double totalPrice;

    @Column(nullable = false)
    private LocalDateTime purchasedAt;

    protected OrderHistoryItem() {
    }

    public OrderHistoryItem(UserProfile user, UUID eventId, String eventName, String seatLabel, Double totalPrice) {
        this.user = user;
        this.eventId = eventId;
        this.eventName = eventName;
        this.seatLabel = seatLabel;
        this.totalPrice = totalPrice;
        this.purchasedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UserProfile getUser() {
        return user;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }
}
