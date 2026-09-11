package com.ticket.booking.application;

import com.ticket.booking.domain.Booking;
import com.ticket.booking.domain.BookingStatus;
import com.ticket.booking.domain.EventSeat;
import com.ticket.booking.infrastructure.BookingRepository;
import com.ticket.booking.infrastructure.EventSeatRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BookingExpirationService {
    private final BookingRepository bookingRepository;
    private final EventSeatRepository eventSeatRepository;

    public BookingExpirationService(BookingRepository bookingRepository, EventSeatRepository eventSeatRepository) {
        this.bookingRepository = bookingRepository;
        this.eventSeatRepository = eventSeatRepository;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void releaseExpiredBookings() {
        for (Booking booking : bookingRepository.findByStatusAndExpiresAtBefore(BookingStatus.RESERVED, LocalDateTime.now())) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            eventSeatRepository.findByEventIdAndSeatRowAndSeatNumber(booking.getEventId(), booking.getSeatRow(), booking.getSeatNumber()).ifPresent(seat -> {seat.markAsAvailable();eventSeatRepository.save(seat);});
        }
    }
}