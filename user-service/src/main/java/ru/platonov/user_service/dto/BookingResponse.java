package ru.platonov.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BookingResponse {
    private String subject;
    private LocalDateTime bookingTime;
    private String status;

}
