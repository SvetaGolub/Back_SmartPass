package org.Proect.domian.Attendance.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.Proect.domian.Attendance.dto.AttendanceRecordRequest;
import org.Proect.domian.Attendance.dto.AttendanceRecordResponse;
import org.Proect.domian.Attendance.service.AttendanceRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AttendanceRecordController {

    private final AttendanceRecordService attendanceService;

    @GetMapping
    public List<AttendanceRecordResponse> getForMonth(
            @PathVariable Integer userId, @RequestParam int year, @RequestParam int month) {
        return attendanceService.getForMonth(userId, year, month);
    }

    @PutMapping("/{date}")
    public AttendanceRecordResponse save(
            @PathVariable Integer userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody AttendanceRecordRequest request) {
        return attendanceService.save(userId, date, request);
    }

    @DeleteMapping("/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Integer userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        attendanceService.delete(userId, date);
    }
}
