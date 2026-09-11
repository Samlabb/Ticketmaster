package com.ticket.booking.application;

import com.ticket.booking.domain.Booking;
import com.ticket.booking.domain.BookingStatus;
import com.ticket.booking.domain.EventSeat;
import com.ticket.booking.domain.SeatStatus;
import com.ticket.booking.infrastructure.BookingProducer;
import com.ticket.booking.infrastructure.BookingRepository;
import com.ticket.booking.infrastructure.EventSeatRepository;
import com.ticket.booking.infrastructure.TicketReturnProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Mock
    private RedisLockService redisLockService;

    @Mock
    private BookingProducer bookingProducer;

    @Mock
    private TicketReturnProducer ticketReturnProducer;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void shouldReserveSeatAndPublishBookingEvent() {
        UUID eventId = UUID.randomUUID();
        EventSeat seat = EventSeat.createProjection(eventId, "A", 1, 5000.0);
        Booking savedBooking = Booking.create(eventId, "user-1", "A", 1);

        when(redisLockService.tryLock(any(), eq("user-1"), any())).thenReturn(true);
        when(eventSeatRepository.findByEventIdAndSeatRowAndSeatNumber(eventId, "A", 1))
                .thenReturn(Optional.of(seat));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        UUID bookingId = bookingService.createBooking(eventId, "user-1", "A", 1);

        assertEquals(savedBooking.getId(), bookingId);
        assertEquals(SeatStatus.RESERVED, seat.getStatus());
        verify(bookingProducer).publishBookingCreated(any());
        verify(redisLockService).releaseLock("lock:booking:" + eventId + ":A:1", "user-1");
    }

    @Test
    void shouldReturnPaidBookingAndPublishCompensationEvent() {
        UUID eventId = UUID.randomUUID();
        Booking booking = Booking.create(eventId, "user-1", "A", 2);
        EventSeat seat = EventSeat.createProjection(eventId, "A", 2, 5000.0);
        seat.markAsReserved();
        seat.markAsSold();

        booking.setStatus(BookingStatus.PAID);
        when(bookingRepository.findByEventIdAndUserIdAndSeatRowAndSeatNumber(eventId, "user-1", "A", 2))
                .thenReturn(Optional.of(booking));
        when(eventSeatRepository.findByEventIdAndSeatRowAndSeatNumber(eventId, "A", 2))
                .thenReturn(Optional.of(seat));

        Booking returned = bookingService.returnTicket(eventId, "user-1", "A", 2);

        assertEquals(BookingStatus.CANCELLED, returned.getStatus());
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        verify(ticketReturnProducer).publish(any());
    }

    @Test
    void shouldRejectReturnForAnotherUser() {
        UUID eventId = UUID.randomUUID();
        when(bookingRepository.findByEventIdAndUserIdAndSeatRowAndSeatNumber(eventId, "user-2", "A", 3))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> bookingService.returnTicket(eventId, "user-2", "A", 3));
    }
}