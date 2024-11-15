package ru.platonov.user_service.exception;

public class SubjectNotFoundException extends RuntimeException{
    public SubjectNotFoundException(String message) {
        super(message);
    }
}
