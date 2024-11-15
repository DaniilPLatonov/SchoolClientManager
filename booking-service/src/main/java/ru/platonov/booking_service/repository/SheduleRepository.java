package ru.platonov.booking_service.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.platonov.booking_service.model.Shedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SheduleRepository extends JpaRepository<Shedule, UUID> {
    List<Shedule> findByTutorId(UUID tutorId);
    List<Shedule> findByTutorIdAndBookedFalse(UUID tutorId);
    boolean existsByTutorIdAndSubjectIdAndDateAndStartTime(UUID tutorId, UUID subjectId, LocalDate date, LocalTime startTime);

}

