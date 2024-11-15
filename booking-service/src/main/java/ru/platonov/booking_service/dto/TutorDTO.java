package ru.platonov.booking_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TutorDTO {
    private UUID id;
    private String name;
}

