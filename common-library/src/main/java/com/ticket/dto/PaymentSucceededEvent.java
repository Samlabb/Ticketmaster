package com.ticket.dto;

import java.io.Serializable;
import java.util.UUID;

public record PaymentSucceededEvent(
        UUID bookingId,
        String transactionId
) implements Serializable {
}