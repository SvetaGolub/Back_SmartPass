package org.Proect.domian.Attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceRecordResponse(
        Long id,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime entry,
        @JsonFormat(pattern = "HH:mm") LocalTime exit) {
}
