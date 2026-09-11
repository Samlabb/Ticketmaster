package com.ticket.booking.infrastructure;

import com.ticket.booking.domain.Booking;
import com.ticket.booking.domain.BookingStatus;
import com.ticket.booking.domain.EventSeat;
import com.ticket.dto.PaymentFailedEvent;
import com.ticket.dto.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@KafkaListener(topics = "ticketmaster.payments", groupId = "booking-service-group")
public class PaymentResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultConsumer.class);
    private final BookingRepository bookingRepository;
    private final EventSeatRepository eventSeatRepository;

    public PaymentResultConsumer(BookingRepository bookingRepository, EventSeatRepository eventSeatRepository) {
        this.bookingRepository = bookingRepository;
        this.eventSeatRepository = eventSeatRepository;
    }

    @KafkaHandler
    @Transactional
    public void handlePaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Получено событие об успешной оплате брони: {}", event.bookingId());

        bookingRepository.findById(event.bookingId()).ifPresent(booking -> {
            if (booking.getStatus() != BookingStatus.RESERVED) {
                return;
            }
            booking.setStatus(BookingStatus.PAID);
            bookingRepository.save(booking);

            eventSeatRepository.findByEventIdAndSeatRowAndSeatNumber(booking.getEventId(), booking.getSeatRow(), booking.getSeatNumber())
                    .ifPresent(seat -> {
                        seat.markAsSold();
                        eventSeatRepository.save(seat);
                        log.info("Статус места {}-{} для события {} обновлен на SOLD", booking.getSeatRow(), booking.getSeatNumber(), booking.getEventId());
                    });

            log.info("Статус брони {} изменен на PAID", event.bookingId());
        });
    }

    @KafkaHandler
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("Получено событие о провале оплаты брони: {}. Причина: {}", event.bookingId(), event.reason());

        bookingRepository.findById(event.bookingId()).ifPresent(booking -> {
            if (booking.getStatus() != BookingStatus.RESERVED) {
                return;
            }
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            eventSeatRepository.findByEventIdAndSeatRowAndSeatNumber(booking.getEventId(), booking.getSeatRow(), booking.getSeatNumber())
                    .ifPresent(seat -> {
                        seat.markAsAvailable();
                        eventSeatRepository.save(seat);
                        log.info("Статус места {}-{} для события {} обновлен на AVAILABLE", booking.getSeatRow(), booking.getSeatNumber(), booking.getEventId());
                    });

            log.info("Статус брони {} изменен на CANCELLED", event.bookingId());
        });
    }
}