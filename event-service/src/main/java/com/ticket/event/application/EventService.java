package com.ticket.event.application;

import com.ticket.dto.EventDeletedEvent;
import com.ticket.dto.SeatInfo;
import com.ticket.dto.CreatedEvent;
import com.ticket.event.domain.Event;
import com.ticket.event.domain.Seat;

import com.ticket.event.infrastructure.EventProducer;
import com.ticket.event.infrastructure.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {
    private final EventRepository eventRepository;
    private final EventProducer kafkaProducer;

    public EventService(EventRepository eventRepository, EventProducer kafkaProducer) {
        this.eventRepository = eventRepository;
        this.kafkaProducer = kafkaProducer;
    }

    @Transactional
    public UUID createEvent(String name, String artist, String location, LocalDateTime eventDate, List<Seat> seats){
        Event event = Event.create(name, artist, location, eventDate, seats);

        Event saveEvent = eventRepository.save(event);

        List<SeatInfo> seatInfos = saveEvent.getSeats().stream().map(seat -> new SeatInfo(seat.row(), seat.seatNumber(), seat.price())).toList();

        CreatedEvent domainEvent = new CreatedEvent(saveEvent.getId(), saveEvent.getName(), seatInfos, LocalDateTime.now());
        kafkaProducer.publishEventCreated(domainEvent);
        return saveEvent.getId();
    }
//Тут хибер неявно сравнивает сущность и бд и сам генерит запрос(можно явно написать eventRepository.save(event))
    @Transactional
    public UUID deleteEvent(UUID idEvent){
        Event event = eventRepository.findByIdAndDeletedAtIsNull(idEvent)
                .orElseThrow(() -> new EntityNotFoundException("Мероприятие не найдено: " + idEvent));

        event.delete();

        kafkaProducer.publishEventDeleted(new EventDeletedEvent(event.getId(), event.getDeletedAt()));

        return event.getId();
    }
}
