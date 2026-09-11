package com.ticket.analytics.application;

import com.ticket.dto.BookingCreatedEvent;
import com.ticket.dto.PaymentFailedEvent;
import com.ticket.dto.PaymentSucceededEvent;
import com.ticket.dto.TicketReturnedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AnalyticsMetrics {
    private final AtomicLong bookings = new AtomicLong();
    private final AtomicLong successfulPayments = new AtomicLong();
    private final AtomicLong failedPayments = new AtomicLong();
    private final AtomicLong returnedTickets = new AtomicLong();

    @KafkaListener(topics = "ticketmaster.bookings", groupId = "analytics-service-group")
    public void onBookingCreated(BookingCreatedEvent event) {
        bookings.incrementAndGet();
    }

    @KafkaListener(topics = "ticketmaster.payments", groupId = "analytics-service-group")
    public void onPayment(Object event) {
        if (event instanceof PaymentSucceededEvent) {
            successfulPayments.incrementAndGet();
        } else if (event instanceof PaymentFailedEvent) {
            failedPayments.incrementAndGet();
        }
    }

    @KafkaListener(topics = "ticketmaster.ticket-returns", groupId = "analytics-service-group")
    public void onTicketReturned(TicketReturnedEvent event) {
        returnedTickets.incrementAndGet();
    }

    public Map<String, Long> snapshot() {
        return Map.of(
                "bookings", bookings.get(),
                "successfulPayments", successfulPayments.get(),
                "failedPayments", failedPayments.get(),
                "returnedTickets", returnedTickets.get()
        );
    }
}