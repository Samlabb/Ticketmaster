package com.ticket.application;

import com.ticket.dto.BookingCreatedEvent;
import com.ticket.dto.PaymentFailedEvent;
import com.ticket.dto.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Component
public class PaymentProcessor {
    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);
    private static final String RESULT_TOPIC = "ticketmaster.payments";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    public PaymentProcessor(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "ticketmaster.bookings", groupId = "payment-service-group")
    public void processPayment(BookingCreatedEvent event) {
        log.info("Получен запрос на оплату. Бронь: {}, Сумма: {}", event.bookingId(), event.amount());

        try {
            Thread.sleep(1000 + random.nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean isSuccess = random.nextDouble() > 0.2;

        if (isSuccess) {
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("Оплата успешна. Транзакция: {}", transactionId);

            PaymentSucceededEvent successEvent = new PaymentSucceededEvent(event.bookingId(), transactionId);
            kafkaTemplate.send(RESULT_TOPIC, event.bookingId().toString(), successEvent);
        } else {
            String reason = "Недостаточно средств на карте";
            log.warn("Оплата отклонена. Причина: {}", reason);

            PaymentFailedEvent failedEvent = new PaymentFailedEvent(event.bookingId(), reason);
            kafkaTemplate.send(RESULT_TOPIC, event.bookingId().toString(), failedEvent);
        }
    }
}