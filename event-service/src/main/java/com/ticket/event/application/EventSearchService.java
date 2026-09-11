package com.ticket.event.application;

import com.ticket.event.domain.Event;

import java.util.List;

public class EventSearchService {

    public List<Event> filterEvents(List<Event> events, String artist, String location, String date) {
        String artistQuery = artist == null ? "" : artist.trim().toLowerCase();
        String locationQuery = location == null ? "" : location.trim().toLowerCase();
        String dateQuery = date == null ? "" : date.trim();

        return events.stream()
                .filter(event -> artistQuery.isEmpty() || event.getArtist().toLowerCase().contains(artistQuery))
                .filter(event -> locationQuery.isEmpty() || event.getLocation().toLowerCase().contains(locationQuery))
                .filter(event -> dateQuery.isEmpty() || event.getEventDate().toLocalDate().toString().equals(dateQuery))
                .toList();
    }
}
