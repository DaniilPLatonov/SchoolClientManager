package ru.platonov.booking_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.platonov.booking_service.dto.SheduleDTO;
import ru.platonov.booking_service.service.SheduleService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/shedules")
public class SheduleController {

    private final SheduleService sheduleService;

    @Autowired
    public SheduleController(SheduleService sheduleService) {
        this.sheduleService = sheduleService;
    }


    @GetMapping("/tutor/{tutorId}")
    public List<SheduleDTO> getShedulesByTutorId(@PathVariable UUID tutorId) {
        return sheduleService.getShedulesByTutorId(tutorId);
    }

    @PostMapping
    public ResponseEntity<Object> createShedule(@RequestBody SheduleDTO sheduleDTO) {
        if (sheduleService.existsByTutorIdAndSubjectIdAndDateAndTime(
                sheduleDTO.getTutorId(),
                sheduleDTO.getSubjectId(),
                sheduleDTO.getDate(),
                sheduleDTO.getStartTime())) {
            // Возвращаем JSON-объект с сообщением об ошибке
            Map<String, String> errorResponse = Map.of("message", "Расписание с такими параметрами уже существует");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        SheduleDTO createdShedule = sheduleService.createShedule(sheduleDTO);
        return new ResponseEntity<>(createdShedule, HttpStatus.CREATED);
    }


    @GetMapping
    public List<SheduleDTO> getAllShedules() {
        return sheduleService.getAllShedules();
    }

}
