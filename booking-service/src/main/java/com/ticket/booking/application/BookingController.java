package com.ticket.booking.application;

import com.ticket.booking.domain.EventSeat;
import com.ticket.booking.infrastructure.EventSeatRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    private final BookingService bookingService;
    private final EventSeatRepository eventSeatRepository;

    public BookingController(BookingService bookingService, EventSeatRepository eventSeatRepository) {
        this.bookingService = bookingService;
        this.eventSeatRepository = eventSeatRepository;
    }

    public record CreateBookingRequest(UUID eventId, String seatRow, Integer seatNumber) {}
    public record ReturnBookingRequest(UUID eventId, String seatRow, Integer seatNumber) {}
    public record BookingStatusResponse(String status) {}
    public record BookingResponse(UUID bookingId, String status) {}
    public record ReturnBookingResponse(String status, String seatRow, Integer seatNumber) {}
    public record EventSeatStatusResponse(String row, Integer seatNumber, String status) {}

    @GetMapping("/{bookingId}/status")
    public ResponseEntity<BookingStatusResponse> getBookingStatus(
            @PathVariable UUID bookingId,
            @RequestHeader(HEADER_USER_ID) String userId,
            @RequestHeader(value = HEADER_USER_ROLE, defaultValue = "USER") String role) {

        return bookingService.findById(bookingId)
                .filter(booking -> booking.getUserId().equals(userId) || "ADMIN".equals(role))
                .map(booking -> ResponseEntity.ok(new BookingStatusResponse(booking.getStatus().name())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/events/{eventId}/seat-status")
    public ResponseEntity<List<EventSeatStatusResponse>> getEventSeatStatuses(@PathVariable UUID eventId) {
        List<EventSeat> seats = eventSeatRepository.findAllByEventId(eventId);

        List<EventSeatStatusResponse> response = seats.stream()
                .map(seat -> new EventSeatStatusResponse(
                        seat.getSeatRow(),
                        seat.getSeatNumber(),
                        seat.getStatus().name()))
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestHeader(HEADER_USER_ID) String userId,
            @RequestBody CreateBookingRequest request) {

        UUID bookingId = bookingService.createBooking(
                request.eventId(),
                userId,
                request.seatRow(),
                request.seatNumber()
        );

        String status = bookingService.findById(bookingId)
                .map(booking -> booking.getStatus().name())
                .orElse("RESERVED");

        return ResponseEntity.ok(new BookingResponse(bookingId, status));
    }

    @PostMapping("/return")
    public ResponseEntity<ReturnBookingResponse> returnTicket(
            @RequestHeader(HEADER_USER_ID) String userId,
            @RequestBody ReturnBookingRequest request) {

        bookingService.returnTicket(
                request.eventId(),
                userId,
                request.seatRow(),
                request.seatNumber()
        );

        return ResponseEntity.ok(new ReturnBookingResponse("RETURNED", request.seatRow(), request.seatNumber()));
    }
}