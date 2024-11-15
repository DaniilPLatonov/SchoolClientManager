package ru.platonov.user_service.dto;


import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BookingRequest {
    private UUID userId;
    private Long scheduleId;
    private String status;
}
