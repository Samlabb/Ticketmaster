package com.ticket.booking.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "booking_event_seats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "seat_row", "seat_number"})
})
public class EventSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID eventId;
    private String seatRow;
    private Integer seatNumber;
    private Double price;

    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    protected EventSeat() {}

    public static EventSeat createProjection(UUID eventId, String row, Integer seatNumber, Double price) {
        EventSeat seat = new EventSeat();
        seat.eventId = eventId;
        seat.seatRow = row;
        seat.seatNumber = seatNumber;
        seat.price = price;
        seat.status = SeatStatus.AVAILABLE;
        return seat;
    }
    public Double getPrice() {
        return price;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getSeatRow() { return seatRow; }
    public Integer getSeatNumber() { return seatNumber; }
    public SeatStatus getStatus() { return status; }

    public void markAsReserved() {
        if (this.status == SeatStatus.RESERVED) {
            return;
        }
        if (this.status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("Место должно быть свободно для бронирования");
        }
        this.status = SeatStatus.RESERVED;
    }

    public void markAsAvailable() {
        if (this.status == SeatStatus.AVAILABLE) {
            return;
        }
        if (this.status != SeatStatus.RESERVED && this.status != SeatStatus.SOLD) {
            throw new IllegalStateException("Можно освободить только забронированное или оплачиваемое место");
        }
        this.status = SeatStatus.AVAILABLE;
    }

    public void markAsSold() {
        if (this.status == SeatStatus.SOLD) {
            return;
        }
        if (this.status != SeatStatus.RESERVED) {
            throw new IllegalStateException("Место должно быть забронировано перед продажей");
        }
        this.status = SeatStatus.SOLD;
    }
}