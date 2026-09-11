package com.ticket.event.application;

import com.ticket.event.domain.Event;
import com.ticket.event.domain.Seat;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventSearchServiceTest {

    @Test
    void shouldFilterEventsByArtistLocationAndDate() {
        EventSearchService service = new EventSearchService();

        Event event1 = Event.create(
                "Summer Nights",
                "Moscow",
                LocalDateTime.of(2026, 9, 15, 20, 0),
                List.of(new Seat("A", 1, 2000.0))
        );
        event1.setArtist("The Weeknd");

        Event event2 = Event.create(
                "City Lights",
                "Berlin",
                LocalDateTime.of(2026, 9, 17, 19, 0),
                List.of(new Seat("B", 3, 1500.0))
        );
        event2.setArtist("Imagine Dragons");

        List<Event> filtered = service.filterEvents(
                List.of(event1, event2),
                "The Weeknd",
                "Moscow",
                "2026-09-15"
        );

        assertEquals(1, filtered.size());
        assertEquals("Summer Nights", filtered.get(0).getName());
    }
}