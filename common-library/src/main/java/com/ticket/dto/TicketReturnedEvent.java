package com.ticket.dto;

import java.io.Serializable;
import java.util.UUID;

public record TicketReturnedEvent(
        UUID eventId,
        String userId,
        String seatRow,
        Integer seatNumber
) implements Serializable {
}