package org.Proect.domian.Attendance.controller;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Attendance.dto.AttendanceForDateResponse;
import org.Proect.domian.Attendance.service.AttendanceRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SECURITY')")
public class AttendanceOverviewController {

    private final AttendanceRecordService attendanceService;

    @GetMapping
    public List<AttendanceForDateResponse> getForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.getForDate(date);
    }
}
