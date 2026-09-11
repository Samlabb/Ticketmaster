package com.ticket.event.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="events")
public class Event {

    @Id
    private UUID id;

    private String name;

    private String artist;

    private String location;

    private LocalDateTime eventDate;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ElementCollection
    @CollectionTable(name= "event_seats", joinColumns = @JoinColumn(name = "event_id"))
    private List<Seat> seats;

    protected Event() {}

    public static Event create(String name, String location, LocalDateTime eventDate, List<Seat> seats) {
        return create(name, null, location, eventDate, seats);
    }

    public static Event create(String name, String artist, String location, LocalDateTime eventDate, List<Seat> seats){
        if (eventDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Нельзя создать мероприятие в прошлом");
        }
        if (seats == null || seats.isEmpty()) {
            throw new IllegalArgumentException("У мероприятия должны быть места");
        }

        Event event = new Event();
        event.id = UUID.randomUUID();
        event.name = name;
        event.artist = artist != null ? artist : name;
        event.location = location;
        event.eventDate = eventDate;
        event.seats = seats;
        return event;
    }

    public void delete(){
        if(this.deletedAt != null){
            throw new IllegalStateException("Уже удалено");
        }
        if(this.eventDate.isBefore(LocalDateTime.now())){
            throw new IllegalStateException("Нельзя удалить прошедшее мероприятие");
        }
        this.deletedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName(){
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getLocation() {
        return location;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public List<Seat> getSeats(){
        return seats;
    }

    public boolean isDeleted(){
        return deletedAt != null;
    }
    public LocalDateTime getDeletedAt(){
        return deletedAt;
    }

}
