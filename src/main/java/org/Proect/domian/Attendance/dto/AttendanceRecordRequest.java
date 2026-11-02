package org.Proect.domian.Attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record AttendanceRecordRequest(
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime entry,
        @JsonFormat(pattern = "HH:mm") LocalTime exit) {
}
