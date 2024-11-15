package ru.platonov.booking_service.kafka;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.platonov.booking_service.dto.BookingRequest;
import ru.platonov.booking_service.dto.BookingRequestDTO;
import ru.platonov.booking_service.dto.BookingResponse;
import ru.platonov.booking_service.dto.BookingResponse;
import ru.platonov.booking_service.model.Booking;
import ru.platonov.booking_service.model.BookingStatus;
import ru.platonov.booking_service.model.Shedule;
import ru.platonov.booking_service.repository.BookingRepository;
import ru.platonov.booking_service.repository.SheduleRepository;
import ru.platonov.booking_service.service.BookingService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class BookingKafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(BookingKafkaConsumer.class);
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final ObjectMapper objectMapper;
    private final SheduleRepository sheduleRepository;

    public BookingKafkaConsumer(BookingService bookingService, BookingRepository bookingRepository, ObjectMapper objectMapper, SheduleRepository sheduleRepository) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.objectMapper = objectMapper;
        this.sheduleRepository = sheduleRepository;
    }


    @KafkaListener(topics = "booking-requests", groupId = "booking-service")
    @Transactional
    public void handleBookingRequest(@Payload String bookingRequestJson) {
        try {
            BookingRequestDTO bookingRequestDTO = objectMapper.readValue(bookingRequestJson, BookingRequestDTO.class);
            Shedule schedule = sheduleRepository.findById(bookingRequestDTO.getScheduleId())
                    .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
            Booking booking = createBooking(bookingRequestDTO.getUserId(), schedule);
            bookingRepository.save(booking);
            schedule.setBooked(true);
            sheduleRepository.save(schedule);
        } catch (Exception e) {
            logger.error("Error processing booking request message from Kafka.", e);
        }
    }

    private Booking createBooking(UUID userId, Shedule schedule) {
        return Booking.builder()
                .userId(userId)
                .schedule(schedule)
                .bookingTime(LocalDateTime.now())
                .status(BookingStatus.CONFIRMED.getStatus())
                .build();
    }

    @KafkaListener(topics = "cancel-booking", groupId = "booking-service-group")
    public void listenForCancelBooking(String bookingId) {
        String result = bookingService.cancelBookingById(bookingId);
        if (result.equals("Бронирование успешно отменено.")) {
            logger.info("Бронирование с ID {} было успешно отменено.", bookingId);
        } else {
            logger.warn("Отмена бронирования с ID {} не удалась: {}", bookingId, result);
        }
    }
}



