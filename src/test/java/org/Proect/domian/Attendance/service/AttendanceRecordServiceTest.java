package org.Proect.domian.Attendance.service;

import org.Proect.domian.Attendance.dto.AttendanceRecordRequest;
import org.Proect.domian.Attendance.model.AttendanceRecord;
import org.Proect.domian.Attendance.repository.AttendanceRecordRepository;
import org.Proect.domian.User.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceRecordServiceTest {

    @Mock
    private AttendanceRecordRepository attendanceRepository;

    @Mock
    private UserRepository userRepository;

    private AttendanceRecordService service;

    @BeforeEach
    void setUp() {
        service = new AttendanceRecordService(attendanceRepository, userRepository);
    }

    @Test
    void createsOrUpdatesDailyAttendanceWithoutTouchingAccessAudit() {
        LocalDate date = LocalDate.now().minusDays(1);
        when(userRepository.existsById(7)).thenReturn(true);
        when(attendanceRepository.findByUserIdAndDate(7, date)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> {
            AttendanceRecord record = invocation.getArgument(0);
            record.setId(3L);
            return record;
        });

        var result = service.save(7, date, new AttendanceRecordRequest(
                LocalTime.of(9, 0), LocalTime.of(17, 30)));

        ArgumentCaptor<AttendanceRecord> saved = ArgumentCaptor.forClass(AttendanceRecord.class);
        verify(attendanceRepository).save(saved.capture());
        assertAll(
                () -> assertEquals(7, saved.getValue().getUserId()),
                () -> assertEquals(date, saved.getValue().getDate()),
                () -> assertEquals(LocalTime.of(9, 0), saved.getValue().getEntry()),
                () -> assertEquals(LocalTime.of(17, 30), saved.getValue().getExit()),
                () -> assertEquals(3L, result.id()));
    }

    @Test
    void rejectsFutureDatesAndExitNotAfterEntry() {
        when(userRepository.existsById(7)).thenReturn(true);
        LocalDate today = LocalDate.now();

        assertThrows(IllegalArgumentException.class, () -> service.save(
                7, today.plusDays(1), new AttendanceRecordRequest(LocalTime.of(9, 0), null)));
        assertThrows(IllegalArgumentException.class, () -> service.save(
                7, today, new AttendanceRecordRequest(LocalTime.of(9, 0), LocalTime.of(9, 0))));

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void rejectsDeletionWhenAttendanceRecordIsMissing() {
        LocalDate date = LocalDate.now().minusDays(1);
        when(userRepository.existsById(7)).thenReturn(true);
        when(attendanceRepository.findByUserIdAndDate(7, date)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.delete(7, date));
        verify(attendanceRepository, never()).delete(any(AttendanceRecord.class));
    }
}
