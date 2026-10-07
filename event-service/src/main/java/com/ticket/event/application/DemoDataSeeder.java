package com.ticket.event.application;

import com.ticket.event.domain.Seat;
import com.ticket.event.infrastructure.EventRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final int MIN_SEATS = 20;
    private static final int MAX_SEATS = 100;
    private static final int SEATS_PER_ROW = 10;

    private final EventRepository eventRepository;
    private final EventService eventService;
    private final Validator validator;
    private final boolean enabled;

    public DemoDataSeeder(
            EventRepository eventRepository,
            EventService eventService,
            Validator validator,
            @Value("${app.demo-data.enabled:true}") boolean enabled) {
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.validator = validator;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        if (eventRepository.count() > 0) {
            return;
        }

        List<CreateEventRequest> demoEvents = createDemoEvents();
        demoEvents.forEach(this::validate);
        demoEvents.forEach(event -> eventService.createEvent(
                event.name(),
                event.artist(),
                event.location(),
                event.eventDate(),
                event.seats()));
    }

    private List<CreateEventRequest> createDemoEvents() {
        List<String> names = List.of(
                "Neon Skyline", "Afterglow Sessions", "Electric Horizon",
                "Northern Lights Live", "City Pulse", "Midnight Frequency",
                "Summer Signal", "Echoes in Motion", "Golden Hour", "Open Air Stories");

        List<String> artists = List.of(
                "The Glass Satellites", "Mira Sol", "Static Bloom", "June Arcade",
                "Velvet Transit", "Nova Avenue", "The Sunday Signals", "Atlas Parade",
                "Luna Fields", "Paper Satellites");

        List<String> venues = List.of(
                "Brooklyn Steel, New York", "The Wiltern, Los Angeles", "The Salt Shed, Chicago",
                "The Anthem, Washington", "The Fillmore, San Francisco", "House of Blues, Boston",
                "The Van Buren, Phoenix", "Mission Ballroom, Denver", "The Orange Peel, Asheville",
                "Stubb's, Austin");

        List<CreateEventRequest> events = new ArrayList<>();

        for (int index = 0; index < names.size(); index++) {
            double price = 45 + index * 7.5;
            int seatCount = randomEvenSeatCount();
            List<Seat> seats = createSeats(price, seatCount);

            events.add(new CreateEventRequest(
                    names.get(index),
                    artists.get(index),
                    venues.get(index),
                    LocalDate.now().plusDays(21 + index * 7).atTime(19, 30),
                    seats));
        }
        return events;
    }
    private int randomEvenSeatCount() {
        int minPairs = MIN_SEATS / 2;
        int maxPairs = MAX_SEATS / 2;
        int pairs = ThreadLocalRandom.current().nextInt(minPairs, maxPairs + 1);
        return pairs * 2;
    }

    private List<Seat> createSeats(double basePrice, int seatCount) {
        List<Seat> seats = new ArrayList<>(seatCount);

        for (int i = 0; i < seatCount; i++) {
            int rowIndex = i / SEATS_PER_ROW;
            String rowLabel = toRowLabel(rowIndex);
            int seatNumber = i % SEATS_PER_ROW + 1;
            double seatPrice = basePrice + rowIndex * 15;

            seats.add(new Seat(rowLabel, seatNumber, seatPrice));
        }

        return seats;
    }

    private String toRowLabel(int rowIndex) {
        StringBuilder label = new StringBuilder();
        int value = rowIndex;
        do {
            int remainder = value % 26;
            label.insert(0, (char) ('A' + remainder));
            value = value / 26 - 1;
        } while (value >= 0);
        return label.toString();
    }

    private void validate(CreateEventRequest event) {
        Set<ConstraintViolation<CreateEventRequest>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("Некорректные демо-данные: " + details);
        }
    }
}