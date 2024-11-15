package ru.platonov.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TutorDTO {
    private UUID id;  // UUID для идентификации преподавателя
    private String name;  // Имя преподавателя
}
