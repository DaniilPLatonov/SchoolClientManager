package ru.platonov.user_service.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.platonov.user_service.dto.*;
import ru.platonov.user_service.exception.IncorrectPasswordException;
import ru.platonov.user_service.exception.UserNotFoundException;
import ru.platonov.user_service.service.UserService;

import java.util.List;
import java.util.UUID;

import static org.apache.kafka.streams.kstream.EmitStrategy.log;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{subjectId}/tutors")
    public ResponseEntity<List<TutorDTO>> getTutorsBySubject(@PathVariable UUID subjectId) {
        List<TutorDTO> tutors = userService.getTutorsBySubjectId(subjectId);
        return ResponseEntity.ok(tutors);
    }

    @GetMapping("subjects/{subjectId}")
    public ResponseEntity<SubjectDTO> getSubjectById(@PathVariable UUID subjectId) {
        SubjectDTO subjectDTO = userService.getSubjectById(subjectId);
        return ResponseEntity.ok(subjectDTO);
    }

    @GetMapping("tutor/{tutorId}")
    public ResponseEntity<TutorDTO> getTutorById(@PathVariable UUID tutorId) {
        TutorDTO tutorDTO = userService.getTutorById(tutorId);
        return ResponseEntity.ok(tutorDTO);
    }


    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/subjects")
    public List<SubjectDTO> getAllSubjects() {
        return userService.getAllSubjects();
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        try {
            LoginResponseDTO response = userService.authenticateUser(loginDTO);
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException | IncorrectPasswordException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponseDTO(false, "Invalid email or password", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LoginResponseDTO(false, "Ошибка при обработке запроса", null));
        }
    }
}

