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
public class BookingResponse {

    private UUID subjectId;
    private LocalDateTime bookingTime;
    private String status;


}
