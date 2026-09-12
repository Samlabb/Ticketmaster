package com.ticket.booking.infrastructure;

import com.ticket.booking.domain.Booking;
import com.ticket.booking.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    boolean existsByEventIdAndSeatRowAndSeatNumberAndStatus(UUID eventId, String seatRow, Integer seatNumber, BookingStatus status);
    Optional<Booking> findByEventIdAndUserIdAndSeatRowAndSeatNumber(UUID eventId, String userId, String seatRow, Integer seatNumber);
    List<Booking> findByEventIdAndUserId(UUID eventId, String userId);
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime time);
    List<Booking> findByEventIdAndStatusIn(UUID eventId, List<BookingStatus> statuses);
}