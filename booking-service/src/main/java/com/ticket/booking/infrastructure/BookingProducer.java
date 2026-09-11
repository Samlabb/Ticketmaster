package com.ticket.booking.infrastructure;

import com.ticket.dto.BookingCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookingProducer {
    private static final Logger log = LoggerFactory.getLogger(BookingProducer.class);
    private static final String TOPIC = "ticketmaster.bookings";

    private final KafkaTemplate<String, BookingCreatedEvent> kafkaTemplate;

    public BookingProducer(KafkaTemplate<String, BookingCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishBookingCreated(BookingCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.bookingId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Событие отправлено в Kafka");
                    } else {
                        log.error("Ошибка отправки в Kafka", ex);
                    }
                });
    }
}