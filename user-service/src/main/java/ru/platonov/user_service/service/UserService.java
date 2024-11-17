package ru.platonov.user_service.service;


import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.platonov.user_service.dto.*;
import ru.platonov.user_service.exception.*;
import ru.platonov.user_service.model.Subject;
import ru.platonov.user_service.model.Tutor;
import ru.platonov.user_service.model.User;
import ru.platonov.user_service.repository.SubjectRepository;
import ru.platonov.user_service.repository.TutorRepository;
import ru.platonov.user_service.repository.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final TutorRepository tutorRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository, SubjectRepository subjectRepository, TutorRepository tutorRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.tutorRepository = tutorRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void addUser(UserDTO userDTO) {
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Пользователь с таким email уже существует");
        }
        User user = modelMapper.map(userDTO, User.class);
        userRepository.save(user);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> modelMapper.map(user, UserDTO.class))
                .toList();
    }

    public List<TutorDTO> getTutorsBySubjectId(UUID subjectId) {
        List<Tutor> tutors = tutorRepository.findTutorsBySubjectId(subjectId);
        if (tutors.isEmpty()) {
            logger.warn("No tutors found for subject: {}", subjectId);
            throw new EntityNotFoundException("No tutors found for subject: " + subjectId);
        }
        return tutors.stream()
                .map(tutor -> new TutorDTO(tutor.getId(), tutor.getName()))
                .collect(Collectors.toList());
    }

    public SubjectDTO getSubjectById(UUID subjectId) {
        return subjectRepository.findById(subjectId)
                .map(subject -> new SubjectDTO(subject.getId(), subject.getName()))
                .orElseThrow(() -> {
                    logger.warn("Subject not found: {}", subjectId);
                    return new EntityNotFoundException("Subject not found for id: " + subjectId);
                });
    }

    public TutorDTO getTutorById(UUID tutorId) {
        return tutorRepository.findById(tutorId)
                .map(tutor -> new TutorDTO(tutor.getId(), tutor.getName()))
                .orElseThrow(() -> {
                    logger.warn("Tutor not found: {}", tutorId);
                    return new EntityNotFoundException("Tutor not found for id: " + tutorId);
                });
    }



    public List<SubjectDTO> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(subject -> modelMapper.map(subject, SubjectDTO.class))
                .toList();
    }

    public LoginResponseDTO authenticateUser(LoginDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new IncorrectPasswordException();
        }

        return new LoginResponseDTO(true, "Успешный вход", user.getId().toString());
    }
}

