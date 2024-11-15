package ru.platonov.booking_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.platonov.booking_service.dto.SheduleDTO;
import ru.platonov.booking_service.exception.ResourceNotFoundException;
import ru.platonov.booking_service.exception.ScheduleException;
import ru.platonov.booking_service.model.Shedule;
import ru.platonov.booking_service.repository.SheduleRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SheduleService {

    private final SheduleRepository sheduleRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public SheduleService(SheduleRepository sheduleRepository, ObjectMapper objectMapper) {
        this.sheduleRepository = sheduleRepository;
        this.objectMapper = objectMapper;
    }

    public List<SheduleDTO> getShedulesByTutorId(UUID tutorId) {
        List<Shedule> shedules = sheduleRepository.findByTutorIdAndBookedFalse(tutorId);
        if (shedules.isEmpty()) {
            throw new EntityNotFoundException("No available schedules found for tutor with id: " + tutorId);
        }
        return shedules.stream()
                .map(shedule -> objectMapper.convertValue(shedule, SheduleDTO.class))
                .collect(Collectors.toList());
    }


    @Transactional
    public SheduleDTO createShedule(SheduleDTO sheduleDTO) {
        try {
            Shedule shedule = objectMapper.convertValue(sheduleDTO, Shedule.class);
            Shedule savedShedule = sheduleRepository.save(shedule);
            return objectMapper.convertValue(savedShedule, SheduleDTO.class);
        } catch (DataIntegrityViolationException e) {
            throw new ScheduleException("Data integrity violation: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ScheduleException("An error occurred while creating schedule: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public boolean existsByTutorIdAndSubjectIdAndDateAndTime(UUID tutorId, UUID subjectId, LocalDate date, LocalTime startTime) {
        return sheduleRepository.existsByTutorIdAndSubjectIdAndDateAndStartTime(tutorId, subjectId, date, startTime);
    }

    public List<SheduleDTO> getAllShedules() {
        List<Shedule> shedules = sheduleRepository.findAll();
        return shedules.stream()
                .map(shedule -> objectMapper.convertValue(shedule, SheduleDTO.class))
                .collect(Collectors.toList());
    }
}
