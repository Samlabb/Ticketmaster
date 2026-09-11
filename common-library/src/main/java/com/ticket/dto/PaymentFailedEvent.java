package com.ticket.dto;

import java.io.Serializable;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID bookingId,
        String reason
) implements Serializable {
}