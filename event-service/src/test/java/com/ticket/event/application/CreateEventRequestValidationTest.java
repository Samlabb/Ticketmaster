package com.ticket.event.application;

import com.ticket.event.domain.Seat;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateEventRequestValidationTest {

    @Test
    void acceptsValidEventData() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            CreateEventRequest request = new CreateEventRequest(
                    "Neon Skyline",
                    "The Glass Satellites",
                    "Brooklyn Steel, New York",
                    LocalDateTime.now().plusDays(30),
                    List.of(new Seat("A", 1, 64.0)));

            assertTrue(validator.validate(request).isEmpty());
        }
    }

    @Test
    void rejectsInvalidEventAndSeatData() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            CreateEventRequest request = new CreateEventRequest(
                    " ",
                    "Artist",
                    "Venue",
                    LocalDateTime.now().minusDays(1),
                    List.of(new Seat("bad row", 1, 64.0)));

            Set<String> invalidFields = validator.validate(request).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertEquals(Set.of("name", "eventDate", "seats[0].row"), invalidFields);
        }
    }

    @Test
    void rejectsNullSeatNumberWithoutNullPointerException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Seat("A", null, 64.0));

        assertTrue(exception.getMessage().contains("Номер места"));
    }
}