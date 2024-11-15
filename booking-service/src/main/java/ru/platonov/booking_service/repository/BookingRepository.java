package ru.platonov.booking_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.platonov.booking_service.model.Booking;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    @Query("SELECT b FROM Booking b JOIN FETCH b.schedule WHERE b.userId = :userId")
    List<Booking> findByUserId(UUID userId);
}
