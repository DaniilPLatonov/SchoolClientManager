package ru.platonov.user_service.exception;

public class TutorNotFoundException extends RuntimeException {
    public TutorNotFoundException(String message) {
        super(message);
    }
}
