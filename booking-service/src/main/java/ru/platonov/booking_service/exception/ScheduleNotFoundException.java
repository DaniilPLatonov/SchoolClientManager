package ru.platonov.booking_service.exception;

public class ScheduleNotFoundException extends RuntimeException{

    public ScheduleNotFoundException(String message){
        super(message);
    }

}
