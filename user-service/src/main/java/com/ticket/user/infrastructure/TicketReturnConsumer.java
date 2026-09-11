package com.ticket.user.infrastructure;

import com.ticket.dto.TicketReturnedEvent;
import com.ticket.user.application.UserService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TicketReturnConsumer {
    private final UserService userService;

    public TicketReturnConsumer(UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(topics = "ticketmaster.ticket-returns", groupId = "user-service-group")
    public void handle(TicketReturnedEvent event) {
        userService.removeOrder(
                UUID.fromString(event.userId()),
                event.eventId(),
                event.seatRow() + event.seatNumber()
        );
    }
}