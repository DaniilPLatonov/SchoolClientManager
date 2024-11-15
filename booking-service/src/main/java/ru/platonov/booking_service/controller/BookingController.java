package ru.platonov.booking_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.platonov.booking_service.dto.BookingInfoDTO;
import ru.platonov.booking_service.dto.BookingRequest;
import ru.platonov.booking_service.service.BookingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/user/{userId}")
    public List<BookingInfoDTO> getUserBookings(@PathVariable UUID userId) {
        return bookingService.findBookingsByUser(userId);
    }

    @PostMapping
    public ResponseEntity<String> createBooking(@RequestBody BookingRequest bookingRequest) {
        try {
            bookingService.createBooking(bookingRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body("Бронирование создано успешно");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ошибка: " + e.getMessage());
        }
    }

}
