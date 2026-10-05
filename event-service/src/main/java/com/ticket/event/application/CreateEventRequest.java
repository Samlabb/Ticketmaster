package com.ticket.event.application;

import com.ticket.event.domain.Seat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreateEventRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) String artist,
        @NotBlank @Size(max = 200) String location,
        @NotNull @Future LocalDateTime eventDate,
        @NotEmpty @Size(max = 500) List<@Valid Seat> seats) {
}