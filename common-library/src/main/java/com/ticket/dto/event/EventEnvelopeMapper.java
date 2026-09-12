package com.ticket.dto.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;

public final class EventEnvelopeMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private EventEnvelopeMapper() {}

    public static <T> EventEnvelope wrap(String eventType, T payload) {
        return new EventEnvelope(eventType, LocalDateTime.now(), MAPPER.valueToTree(payload));
    }

    public static <T> T unwrap(EventEnvelope envelope, Class<T> targetType) {
        try {
            return MAPPER.treeToValue(envelope.payload(), targetType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Не удалось распаковать payload для типа " + envelope.eventType(), e);
        }
    }
}