package ru.platonov.booking_service.model;

import lombok.Getter;

@Getter
public enum BookingStatus {
    CONFIRMED("CONFIRMED"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    private final String status;

    BookingStatus(String status) {
        this.status = status;
    }

}