package com.ticket.booking.infrastructure;

import com.ticket.dto.TicketReturnedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TicketReturnProducer {
    private static final Logger log = LoggerFactory.getLogger(TicketReturnProducer.class);
    private static final String TOPIC = "ticketmaster.ticket-returns";

    private final KafkaTemplate<String, TicketReturnedEvent> kafkaTemplate;

    public TicketReturnProducer(KafkaTemplate<String, TicketReturnedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TicketReturnedEvent event) {
        String key = event.userId() + ":" + event.eventId() + ":" + event.seatRow() + event.seatNumber();
        kafkaTemplate.send(TOPIC, key, event).whenComplete((result, error) -> {
            if (error != null) {
                log.error("Не удалось отправить событие возврата билета", error);
            } else {
                log.info("Событие возврата билета отправлено в Kafka");
            }
        });
    }
}