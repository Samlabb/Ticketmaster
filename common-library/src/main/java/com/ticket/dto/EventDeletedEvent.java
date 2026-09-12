package com.ticket.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventDeletedEvent(
        UUID eventId,
        LocalDateTime deletedAt
) implements Serializable {
}