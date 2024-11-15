package ru.platonov.telegram_bot_service.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SheduleDTO {

    private UUID id;

    private UUID tutorId;

    private UUID subjectId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime endTime;

    @JsonProperty("isBooked")
    private boolean booked;

    @Override
    public String toString() {
        return "SheduleDTO{" +
                "id=" + id +
                ", tutorId=" + tutorId +
                ", subjectId=" + subjectId +
                ", date=" + date +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", isBooked=" + booked +
                '}';
    }

}

