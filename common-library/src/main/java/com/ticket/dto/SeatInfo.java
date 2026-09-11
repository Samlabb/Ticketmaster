package com.ticket.dto;

import java.io.Serializable;

public record SeatInfo(
        String row,
        Integer seatNumber,
        Double price
) implements Serializable {
}