package com.ticket.booking.infrastructure;

import com.ticket.booking.domain.Booking;
import com.ticket.booking.domain.BookingStatus;
import com.ticket.booking.domain.EventSeat;
import com.ticket.dto.CreatedEvent;
import com.ticket.dto.EventDeletedEvent;
import com.ticket.dto.RefundRequestedEvent;
import com.ticket.dto.TicketReturnedEvent;
import com.ticket.dto.event.EventEnvelope;
import com.ticket.dto.event.EventEnvelopeMapper;
import com.ticket.dto.event.EventTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EventLifecycleConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventLifecycleConsumer.class);

    private final EventSeatRepository eventSeatRepository;
    private final BookingRepository bookingRepository;
    private final RefundProducer refundProducer;
    private final TicketReturnProducer ticketReturnProducer;

    public EventLifecycleConsumer(EventSeatRepository eventSeatRepository,
                                  BookingRepository bookingRepository,
                                  RefundProducer refundProducer,
                                  TicketReturnProducer ticketReturnProducer) {
        this.eventSeatRepository = eventSeatRepository;
        this.bookingRepository = bookingRepository;
        this.refundProducer = refundProducer;
        this.ticketReturnProducer = ticketReturnProducer;
    }
    //Обработка разных событий из кафки
    @KafkaListener(topics = {"ticketmaster.events"}, groupId = "booking-service-group")
    @Transactional
    public void listen(EventEnvelope envelope) {
        switch (envelope.eventType()) {
            case EventTypes.EVENT_CREATED -> handleCreated(EventEnvelopeMapper.unwrap(envelope, CreatedEvent.class));
            case EventTypes.EVENT_DELETED -> handleDeleted(EventEnvelopeMapper.unwrap(envelope, EventDeletedEvent.class));
            default ->
                    log.warn("Неизвестный тип события в топике ticketmaster.events: {}", envelope.eventType());
        }
    }

    private void handleCreated(CreatedEvent event) {
        List<EventSeat> seatsToSave = event.seats().stream()
                .map(seatDto -> EventSeat.createProjection(event.eventId(), seatDto.row(), seatDto.seatNumber(), seatDto.price()))
                .toList();

        eventSeatRepository.saveAll(seatsToSave);
        log.info("Проекция из {} мест сохранена для события {}", seatsToSave.size(), event.eventId());
    }

    private void handleDeleted(EventDeletedEvent event) {
        List<EventSeat> seats = eventSeatRepository.findAllByEventId(event.eventId());
        Map<String, EventSeat> seatByKey = seats.stream()
                .collect(Collectors.toMap(seat -> seatKey(seat.getSeatRow(), seat.getSeatNumber()), Function.identity()));

        List<Booking> activeBookings = bookingRepository.findByEventIdAndStatusIn(
                event.eventId(), List.of(BookingStatus.RESERVED, BookingStatus.PAID));

        for (Booking booking : activeBookings) {
            BookingStatus previousStatus = booking.getStatus();
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            if (previousStatus == BookingStatus.PAID) {
                EventSeat seat = seatByKey.get(seatKey(booking.getSeatRow(), booking.getSeatNumber()));
                double amount = seat != null ? seat.getPrice() : 0.0;

                refundProducer.publish(new RefundRequestedEvent(
                        booking.getId(), event.eventId(), booking.getUserId(), amount));

                ticketReturnProducer.publish(new TicketReturnedEvent(
                        event.eventId(), booking.getUserId(), booking.getSeatRow(), booking.getSeatNumber()));

                log.info("Возврат средств по брони {} на сумму {} сделан", booking.getId(), amount);
            } else {
                log.info("Бронь {} отменена без возврата средств (потому что статус PAID) ", booking.getId());
            }
        }

        eventSeatRepository.deleteAll(seats);
        log.info("Удалена проекция мест ({} шт.) для мероприятия {}", seats.size(), event.eventId());
    }

    private String seatKey(String row, Integer seatNumber) {
        return row + "-" + seatNumber;
    }
}