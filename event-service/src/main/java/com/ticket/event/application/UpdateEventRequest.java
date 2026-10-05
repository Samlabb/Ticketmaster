package com.ticket.event.application;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateEventRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) String artist,
        @NotBlank @Size(max = 200) String location,
        @NotNull @Future LocalDateTime eventDate) {
}