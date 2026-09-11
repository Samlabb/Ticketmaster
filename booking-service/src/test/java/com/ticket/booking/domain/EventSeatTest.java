package com.ticket.booking.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventSeatTest {

    @Test
    void shouldReleaseReservedSeatBackToAvailable() {
        EventSeat seat = EventSeat.createProjection(UUID.randomUUID(), "A", 10, 2500.0);

        seat.markAsReserved();
        seat.markAsAvailable();

        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
    }

    @Test
    void shouldMarkSeatSoldAfterSuccessfulPayment() {
        EventSeat seat = EventSeat.createProjection(UUID.randomUUID(), "A", 11, 2500.0);

        seat.markAsReserved();
        seat.markAsSold();

        assertEquals(SeatStatus.SOLD, seat.getStatus());
        assertThrows(IllegalStateException.class, seat::markAsReserved);
    }

    @Test
    void shouldReleaseSoldSeatBackToAvailableWhenTicketIsReturned() {
        EventSeat seat = EventSeat.createProjection(UUID.randomUUID(), "A", 12, 2500.0);

        seat.markAsReserved();
        seat.markAsSold();
        seat.markAsAvailable();

        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
    }

    @Test
    void shouldIgnoreAlreadyAvailableSeatWhenReturningTicket() {
        EventSeat seat = EventSeat.createProjection(UUID.randomUUID(), "A", 13, 2500.0);

        seat.markAsAvailable();

        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
    }

    @Test
    void shouldNotThrowWhenSeatIsAlreadyAvailableDuringReturn() {
        EventSeat seat = EventSeat.createProjection(UUID.randomUUID(), "A", 14, 2500.0);

        assertDoesNotThrow(seat::markAsAvailable);
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
    }
}
