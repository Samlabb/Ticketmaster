package com.ticket.booking.application;

import com.ticket.booking.domain.Booking;
import com.ticket.booking.domain.BookingStatus;
import com.ticket.booking.domain.EventSeat;
import com.ticket.booking.infrastructure.TicketReturnProducer;
import com.ticket.booking.infrastructure.BookingProducer;
import com.ticket.booking.infrastructure.BookingRepository;
import com.ticket.booking.infrastructure.EventSeatRepository;
import com.ticket.dto.BookingCreatedEvent;
import com.ticket.dto.TicketReturnedEvent;
import com.ticket.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventSeatRepository eventSeatRepository;
    private final RedisLockService redisLockService;
    private final BookingProducer bookingProducer;
    private final TicketReturnProducer ticketReturnProducer;

    public BookingService(BookingRepository bookingRepository,
                          EventSeatRepository eventSeatRepository,
                          RedisLockService redisLockService,
                          BookingProducer bookingProducer,
                          TicketReturnProducer ticketReturnProducer) {
        this.bookingRepository = bookingRepository;
        this.eventSeatRepository = eventSeatRepository;
        this.redisLockService = redisLockService;
        this.bookingProducer = bookingProducer;
        this.ticketReturnProducer = ticketReturnProducer;
    }

    public Optional<com.ticket.booking.domain.Booking> findById(UUID bookingId) {
        return bookingRepository.findById(bookingId);
    }

    @Transactional
    public UUID createBooking(UUID eventId, String userId, String seatRow, Integer seatNumber) {
        String lockKey = "lock:booking:" + eventId + ":" + seatRow + ":" + seatNumber;
        String lockValue = userId;
        Duration lockTimeout = Duration.ofMinutes(15);

        boolean isLocked = redisLockService.tryLock(lockKey, lockValue, lockTimeout);
        if (!isLocked) {
            throw new BusinessException("Это место уже резервируется другим пользователем", HttpStatus.CONFLICT, "место уже резервируется");
        }

        try {
            EventSeat seat = eventSeatRepository.findByEventIdAndSeatRowAndSeatNumber(eventId, seatRow, seatNumber).orElseThrow(() -> new BusinessException("Место не найдено", HttpStatus.NOT_FOUND, "мето не найдено"));

            if (seat.getStatus() != com.ticket.booking.domain.SeatStatus.AVAILABLE) {
                throw new BusinessException("Место уже забронировано или продано", HttpStatus.CONFLICT, "место уже забронировано или продано");
            }

            seat.markAsReserved();
            eventSeatRepository.save(seat);
            Booking booking = Booking.create(eventId, userId, seatRow, seatNumber, seat.getPrice());
            Booking savedBooking = bookingRepository.save(booking);

            BookingCreatedEvent kafkaEvent = new BookingCreatedEvent(
                    savedBooking.getId(),
                    eventId,
                    userId,
                    seat.getPrice()
            );

            bookingProducer.publishBookingCreated(kafkaEvent);

            return savedBooking.getId();

        } finally {
            redisLockService.releaseLock(lockKey, lockValue);
        }
    }

    @Transactional
    public Booking returnTicket(UUID eventId, String userId, String seatRow, Integer seatNumber) {
        Booking booking = bookingRepository.findByEventIdAndUserIdAndSeatRowAndSeatNumber(eventId, userId, seatRow, seatNumber)
                .orElseThrow(() -> new BusinessException("Билет не найден для этого пользователя", HttpStatus.NOT_FOUND, "билет не найден"));

        if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.RESERVED) {
            throw new BusinessException("Этот билет нельзя вернуть", HttpStatus.CONFLICT, "билет нельзя вернуть");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        EventSeat seat = eventSeatRepository.findByEventIdAndSeatRowAndSeatNumber(eventId, seatRow, seatNumber).orElseThrow(() -> new BusinessException("Место не найдено", HttpStatus.NOT_FOUND, "место не найдено"));

        seat.markAsAvailable();
        eventSeatRepository.save(seat);

        ticketReturnProducer.publish(new TicketReturnedEvent(eventId, userId, seatRow, seatNumber));

        return booking;
    }
}