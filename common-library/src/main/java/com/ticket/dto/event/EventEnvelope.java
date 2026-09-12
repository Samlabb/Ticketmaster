package com.ticket.dto.event;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;
import java.time.LocalDateTime;

public record EventEnvelope(
        String eventType,
        LocalDateTime occurredAt,
        JsonNode payload
) implements Serializable {
}