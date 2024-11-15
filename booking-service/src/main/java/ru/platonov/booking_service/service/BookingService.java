package ru.platonov.booking_service.service;


import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.platonov.booking_service.dto.*;
import ru.platonov.booking_service.exception.ScheduleNotFoundException;
import ru.platonov.booking_service.model.Booking;
import ru.platonov.booking_service.model.Shedule;
import ru.platonov.booking_service.repository.BookingRepository;
import ru.platonov.booking_service.repository.SheduleRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SheduleRepository sheduleRepository;
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    private final RestTemplate restTemplate;
    private static final String SUBJECT_ENDPOINT = "http://user-service:8080/api/users/subjects/{subjectId}";
    private static final String TUTOR_ENDPOINT = "http://user-service:8080/api/users/tutor/{tutorId}";

    @Autowired
    public BookingService(BookingRepository bookingRepository, SheduleRepository sheduleRepository, RestTemplate restTemplate) {
        this.bookingRepository = bookingRepository;
        this.sheduleRepository = sheduleRepository;

        this.restTemplate = restTemplate;
    }

    public void createBooking(BookingRequest request) {
        Shedule schedule = sheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ScheduleNotFoundException("Расписание с ID " + request.getScheduleId() + " не найдено"));

        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setSchedule(schedule);
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus(request.getStatus());
        bookingRepository.save(booking);
        logger.info("Booking created successfully for user ID: {}", request.getUserId());

    }


    public List<BookingInfoDTO> findBookingsByUser(UUID userId) {
        logger.info("Fetching bookings for user ID: {}", userId);
        List<Booking> bookings = bookingRepository.findByUserId(userId);

        if (bookings.isEmpty()) {
            logger.warn("No bookings found for user ID: {}", userId);
            throw new EntityNotFoundException("No bookings found for user ID: " + userId);
        }

        return bookings.stream()
                .map(this::convertToBookingInfoDTO)
                .collect(Collectors.toList());
    }


    private BookingInfoDTO convertToBookingInfoDTO(Booking booking) {
        Shedule schedule = booking.getSchedule();
        String subjectName = getSubjectNameById(schedule.getSubjectId());
        String tutorName = getTutorNameById(schedule.getTutorId());

        return BookingInfoDTO.builder()
                .bookingId(String.valueOf(booking.getId()))
                .subjectName(subjectName)
                .tutorName(tutorName)
                .date(schedule.getDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .build();
    }



/*    public List<BookingInfoDTO> findBookingsByUser(UUID userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookings.stream().map(booking -> {
            Shedule schedule = booking.getSchedule();
            String subjectName = getSubjectNameById(schedule.getSubjectId());
            String tutorName = getTutorNameById(schedule.getTutorId());
            return BookingInfoDTO.builder()
                    .bookingId(String.valueOf(booking.getId()))
                    .subjectName(subjectName)
                    .tutorName(tutorName)
                    .date(schedule.getDate())
                    .startTime(schedule.getStartTime())
                    .endTime(schedule.getEndTime())
                    .build();
        }).collect(Collectors.toList());
    }*/



    private String getSubjectNameById(UUID subjectId) {
        try {
            SubjectDTO subject = restTemplate.getForObject(SUBJECT_ENDPOINT, SubjectDTO.class, subjectId);
            return subject != null ? subject.getName() : "Unknown Subject";
        } catch (Exception e) {
            logger.error("Error fetching subject with ID: {}. Error: {}", subjectId, e.getMessage());
            return "Error fetching subject";
        }
    }


    private String getTutorNameById(UUID tutorId) {
        try {
            TutorDTO tutor = restTemplate.getForObject(TUTOR_ENDPOINT, TutorDTO.class, tutorId);
            return tutor != null ? tutor.getName() : "Unknown Tutor";
        } catch (Exception e) {
            logger.error("Error fetching tutor with ID: {}. Error: {}", tutorId, e.getMessage());
            return "Error fetching tutor";
        }
    }


    @Transactional
    public String cancelBookingById(String bookingId) {
        try {
            Booking booking = bookingRepository.findById(UUID.fromString(bookingId))
                    .orElseThrow(() -> new IllegalArgumentException("Бронирование с ID " + bookingId + " не найдено"));
            Shedule schedule = booking.getSchedule();
            if (schedule == null) {
                throw new IllegalStateException("Связанное расписание не найдено для бронирования с ID " + bookingId);
            }
            schedule.setBooked(false);
            sheduleRepository.save(schedule);
            bookingRepository.delete(booking);
            return "Бронирование успешно отменено.";

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Ошибка: {}", e.getMessage());
            return e.getMessage();

        } catch (Exception e) {
            logger.error("Ошибка при отмене бронирования с ID {}: {}", bookingId, e.getMessage());
            return "Произошла ошибка при отмене бронирования. Попробуйте позже.";
        }
    }

}
