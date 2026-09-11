package com.ticket.event.infrastructure;

import com.ticket.dto.CreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {
    private static final Logger log = LoggerFactory.getLogger(EventProducer.class);
    private static final String TOPIC = "ticketmaster.events";

    private final KafkaTemplate<String, CreatedEvent> kafkaTemplate;

    public EventProducer(KafkaTemplate<String, CreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEventCreated(CreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.eventId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Событие отправлено в Кафку: partition={}, offset={}",
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Ошибка отправки в Кафку", ex);
                    }
                });
    }
}
