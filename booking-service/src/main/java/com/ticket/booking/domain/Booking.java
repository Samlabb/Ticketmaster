package com.ticket.booking.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID eventId;
    private String userId;
    private String seatRow;
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    protected Booking() {}

    public static Booking create(UUID eventId, String userId, String seatRow, Integer seatNumber) {
        Booking booking = new Booking();
        booking.id = UUID.randomUUID();
        booking.eventId = eventId;
        booking.userId = userId;
        booking.seatRow = seatRow;
        booking.seatNumber = seatNumber;
        booking.status = BookingStatus.RESERVED;
        booking.createdAt = LocalDateTime.now();
        booking.expiresAt = LocalDateTime.now().plusMinutes(15);
        return booking;
    }

    public UUID getId(){
        return id;
    }

    public UUID getEventId(){
        return eventId;
    }

    public String getSeatRow() {
        return seatRow;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setStatus(BookingStatus status){
        this.status=status;
    }

    public BookingStatus getStatus(){
        return status;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}