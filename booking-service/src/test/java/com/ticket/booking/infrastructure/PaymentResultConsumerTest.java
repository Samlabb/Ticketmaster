package com.ticket.booking.infrastructure;

import com.ticket.booking.domain.Booking;
import com.ticket.booking.domain.BookingStatus;
import com.ticket.booking.domain.EventSeat;
import com.ticket.booking.domain.SeatStatus;
import com.ticket.dto.PaymentFailedEvent;
import com.ticket.dto.PaymentSucceededEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentResultConsumerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Test
    void shouldMarkBookingPaidAndSeatSoldAfterSuccessfulPayment() {
        UUID eventId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.create(eventId, "user-123123", "A", 4, 5000.0);
        EventSeat seat = EventSeat.createProjection(eventId, "A", 4, 5000.0);
        seat.markAsReserved();

        when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));
        when(eventSeatRepository.findByEventIdAndSeatRowAndSeatNumber(eventId, "A", 4))
                .thenReturn(Optional.of(seat));

        new PaymentResultConsumer(bookingRepository, eventSeatRepository)
                .handlePaymentSucceeded(new PaymentSucceededEvent(bookingId, "TXN-123123"));

        assertEquals(BookingStatus.PAID, booking.getStatus());
        assertEquals(SeatStatus.SOLD, seat.getStatus());
        verify(bookingRepository).save(booking);
        verify(eventSeatRepository).save(seat);
    }

    @Test
    void shouldCancelBookingAndReleaseSeatAfterFailedPayment() {
        UUID eventId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.create(eventId, "user-123123", "A", 5, 5000.0);
        EventSeat seat = EventSeat.createProjection(eventId, "A", 5, 5000.0);
        seat.markAsReserved();

        when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));
        when(eventSeatRepository.findByEventIdAndSeatRowAndSeatNumber(eventId, "A", 5))
                .thenReturn(Optional.of(seat));

        new PaymentResultConsumer(bookingRepository, eventSeatRepository)
                .handlePaymentFailed(new PaymentFailedEvent(bookingId, "declined"));

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        verify(bookingRepository).save(booking);
        verify(eventSeatRepository).save(seat);
    }
}