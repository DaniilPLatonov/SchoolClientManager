package ru.platonov.user_service.service;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.platonov.user_service.dto.SubjectDTO;
import ru.platonov.user_service.dto.TutorDTO;
import ru.platonov.user_service.dto.UserDTO;
import ru.platonov.user_service.exception.SubjectNotFoundException;
import ru.platonov.user_service.exception.TutorNotFoundException;
import ru.platonov.user_service.exception.UserAlreadyExistsException;
import ru.platonov.user_service.exception.UserNotFoundException;
import ru.platonov.user_service.model.Subject;
import ru.platonov.user_service.model.Tutor;
import ru.platonov.user_service.model.User;
import ru.platonov.user_service.repository.SubjectRepository;
import ru.platonov.user_service.repository.TutorRepository;
import ru.platonov.user_service.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final SubjectRepository subjectRepository;
    private final ModelMapper modelMapper;

    public AdminService(UserRepository userRepository, TutorRepository tutorRepository, SubjectRepository subjectRepository, ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.tutorRepository = tutorRepository;
        this.subjectRepository = subjectRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public void addUser(UserDTO userDTO) {
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Пользователь с таким email уже существует");
        }
        User user = modelMapper.map(userDTO, User.class);
        userRepository.save(user);
    }

    public void removeUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("Пользователь с ID " + userId + " не найден");
        }
        userRepository.deleteById(userId);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> modelMapper.map(user, UserDTO.class))
                .toList();
    }

    @Transactional
    public void addTutor(TutorDTO tutorDTO) {
        Tutor tutor = modelMapper.map(tutorDTO, Tutor.class);
        tutorRepository.save(tutor);
    }

    public void removeTutor(UUID tutorId) {
        tutorRepository.findById(tutorId)
                .orElseThrow(() -> new TutorNotFoundException("Преподаватель с ID " + tutorId + " не найден"));
        tutorRepository.deleteById(tutorId);
    }

    public List<TutorDTO> getAllTutors() {
        return tutorRepository.findAll().stream()
                .map(tutor -> modelMapper.map(tutor, TutorDTO.class))
                .toList();
    }

    @Transactional
    public void addSubject(SubjectDTO subjectDTO) {
        Subject subject = modelMapper.map(subjectDTO, Subject.class);
        subjectRepository.save(subject);
    }

    public void removeSubject(UUID subjectId) {
        subjectRepository.findById(subjectId)
                .orElseThrow(() -> new SubjectNotFoundException("Предмет с ID " + subjectId + " не найден"));
        subjectRepository.deleteById(subjectId);
    }

    public List<SubjectDTO> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(subject -> modelMapper.map(subject, SubjectDTO.class))
                .toList();
    }
}


