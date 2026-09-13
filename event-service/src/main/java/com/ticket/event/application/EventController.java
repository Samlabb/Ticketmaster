package com.ticket.event.application;

import com.ticket.event.domain.Event;
import com.ticket.event.domain.Seat;
import com.ticket.event.infrastructure.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;

    public EventController(EventService eventService, EventRepository eventRepository) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
    }

    public record CreateEventRequest(String name, String artist, String location, LocalDateTime eventDate, List<Seat> seats) {}
    public record EventResponse(UUID id, String name, String artist, String location, LocalDateTime eventDate, List<Seat> seats) {}

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents(
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String date
    ) {
        List<EventResponse> events = eventRepository.findAllActive().stream()
                .filter(event -> artist == null || artist.isBlank() || event.getArtist().toLowerCase().contains(artist.toLowerCase()))
                .filter(event -> location == null || location.isBlank() || event.getLocation().toLowerCase().contains(location.toLowerCase()))
                .filter(event -> date == null || date.isBlank() || event.getEventDate().toLocalDate().toString().equals(date))
                .map(event -> new EventResponse(event.getId(), event.getName(), event.getArtist(), event.getLocation(), event.getEventDate(), event.getSeats())).toList();

        return ResponseEntity.ok(events);
    }

    @PostMapping
    public ResponseEntity<UUID> createEvent(@RequestBody CreateEventRequest request) {
        UUID eventId = eventService.createEvent(
                request.name(),
                request.artist(),
                request.location(),
                request.eventDate(),
                request.seats()
        );
        return ResponseEntity.ok(eventId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UUID> deleteEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.deleteEvent(id));
    }
}