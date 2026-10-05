package com.ticket.event.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

@Embeddable
public record Seat (
    @NotBlank @Pattern(regexp = "[A-Z]{1,2}")
    String row,
    @Positive
    Integer seatNumber,
    @Positive
    Double price)
{
    public Seat {
        if (price == null || !Double.isFinite(price) || price <= 0) {
            throw new IllegalArgumentException("Цена должна быть положительным числом");
        }
        if (seatNumber == null || seatNumber <= 0) {
            throw new IllegalArgumentException("Номер места должен быть положительным числом");
        }
    }
}
