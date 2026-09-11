package com.ticket.event.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public record Seat (
    String row,
    Integer seatNumber,
    Double price)
{
    public Seat {
        if(price <=0) throw new IllegalArgumentException("Цена что-то низковата");
        if(seatNumber<0) throw new IllegalArgumentException("В подвале мест не продаем");
    }
}
