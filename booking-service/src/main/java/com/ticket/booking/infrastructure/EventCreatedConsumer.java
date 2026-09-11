package com.ticket.booking.infrastructure;

import com.ticket.booking.domain.EventSeat;

import com.ticket.dto.CreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class EventCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventCreatedConsumer.class);
    private final EventSeatRepository eventSeatRepository;

    public EventCreatedConsumer(EventSeatRepository eventSeatRepository) {
        this.eventSeatRepository = eventSeatRepository;
    }

    @KafkaListener(topics = "ticketmaster.events", groupId = "booking-service-group")
    @Transactional
    public void listen(CreatedEvent event) {
        log.info("Получено событие о новом мероприятии: ID={}, Название={}, Мест={}",
                event.eventId(), event.eventName(), event.seats().size());

        List<EventSeat> seatsToSave = event.seats().stream().map(seatDto -> EventSeat.createProjection(event.eventId(), seatDto.row(), seatDto.seatNumber(), seatDto.price())).toList();

        eventSeatRepository.saveAll(seatsToSave);
        log.info("Проекция из {} мест  сохранена в бд бронирования", seatsToSave.size());
    }
}