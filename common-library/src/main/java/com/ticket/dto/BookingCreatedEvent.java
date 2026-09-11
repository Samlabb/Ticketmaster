package com.ticket.dto;

import java.io.Serializable;
import java.util.UUID;

public record BookingCreatedEvent(
        UUID bookingId,
        UUID eventId,
        String userId,
        Double amount
) implements Serializable {
}