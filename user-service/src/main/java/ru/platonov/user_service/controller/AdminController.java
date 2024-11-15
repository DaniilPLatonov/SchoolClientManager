package ru.platonov.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.platonov.user_service.dto.SubjectDTO;
import ru.platonov.user_service.dto.TutorDTO;
import ru.platonov.user_service.dto.UserDTO;
import ru.platonov.user_service.service.AdminService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/users")
    public ResponseEntity<Void> addUser(@RequestBody UserDTO userDTO) {
        adminService.addUser(userDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> removeUser(@PathVariable Long userId) {
        adminService.removeUser(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/tutors")
    public ResponseEntity<Void> addTutor(@RequestBody TutorDTO tutorDTO) {
        adminService.addTutor(tutorDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tutors/{tutorId}")
    public ResponseEntity<Void> removeTutor(@PathVariable UUID tutorId) {
        adminService.removeTutor(tutorId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tutors")
    public ResponseEntity<List<TutorDTO>> getAllTutors() {
        List<TutorDTO> tutors = adminService.getAllTutors();
        return ResponseEntity.ok(tutors);
    }

    @PostMapping("/subjects")
    public ResponseEntity<Void> addSubject(@RequestBody SubjectDTO subjectDTO) {
        adminService.addSubject(subjectDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/subjects/{subjectId}")
    public ResponseEntity<Void> removeSubject(@PathVariable UUID subjectId) {
        adminService.removeSubject(subjectId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<SubjectDTO>> getAllSubjects() {
        List<SubjectDTO> subjects = adminService.getAllSubjects();
        return ResponseEntity.ok(subjects);
    }

}
