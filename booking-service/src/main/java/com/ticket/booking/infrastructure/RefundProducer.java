package com.ticket.booking.infrastructure;

import com.ticket.dto.RefundRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RefundProducer {
    private static final Logger log = LoggerFactory.getLogger(RefundProducer.class);
    private static final String TOPIC = "ticketmaster.refunds";

    private final KafkaTemplate<String, RefundRequestedEvent> kafkaTemplate;

    public RefundProducer(KafkaTemplate<String, RefundRequestedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(RefundRequestedEvent event) {
        kafkaTemplate.send(TOPIC, event.bookingId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Не удалось отправить событие возврата средств", error);
                    } else {
                        log.info("Событие возврата средств отправлено в Kafka: бронь {}", event.bookingId());
                    }
                });
    }
}