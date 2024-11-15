package ru.platonov.booking_service.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BookingDTO {
    private UUID id;
    private UUID userId;
    private UUID scheduleId;
    private LocalDateTime bookingTime;
    private String status;
}
