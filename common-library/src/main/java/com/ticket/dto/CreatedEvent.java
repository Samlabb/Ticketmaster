package com.ticket.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreatedEvent(
        UUID eventId,
        String eventName,
        List<SeatInfo> seats,
        LocalDateTime createdAt
) implements Serializable {
}
