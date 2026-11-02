package org.Proect.domian.Attendance.service;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Attendance.dto.AttendanceRecordRequest;
import org.Proect.domian.Attendance.dto.AttendanceRecordResponse;
import org.Proect.domian.Attendance.dto.AttendanceForDateResponse;
import org.Proect.domian.Attendance.model.AttendanceRecord;
import org.Proect.domian.Attendance.repository.AttendanceRecordRepository;
import org.Proect.domian.User.exception.UserNotFoundException;
import org.Proect.domian.User.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceRecordService {

    private final AttendanceRecordRepository attendanceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getForMonth(Integer userId, int year, int month) {
        requireUser(userId);
        YearMonth requestedMonth = YearMonth.of(year, month);
        return attendanceRepository.findByUserIdAndDateBetweenOrderByDateAsc(
                        userId, requestedMonth.atDay(1), requestedMonth.atEndOfMonth())
                .stream()
                .map(AttendanceRecordService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceForDateResponse> getForDate(LocalDate date) {
        return attendanceRepository.findByDateOrderByUserIdAsc(date).stream()
                .map(record -> new AttendanceForDateResponse(
                        record.getUserId(), record.getDate(), record.getEntry(), record.getExit()))
                .toList();
    }

    @Transactional
    public AttendanceRecordResponse save(Integer userId, LocalDate date, AttendanceRecordRequest request) {
        requireUser(userId);
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Нельзя указать будущую дату");
        }
        if (request.exit() != null && !request.exit().isAfter(request.entry())) {
            throw new IllegalArgumentException("Выход должен быть позже входа");
        }

        AttendanceRecord record = attendanceRepository.findByUserIdAndDate(userId, date)
                .orElseGet(AttendanceRecord::new);
        record.setUserId(userId);
        record.setDate(date);
        record.setEntry(request.entry());
        record.setExit(request.exit());
        return toResponse(attendanceRepository.save(record));
    }

    @Transactional
    public void delete(Integer userId, LocalDate date) {
        requireUser(userId);
        AttendanceRecord record = attendanceRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new IllegalArgumentException("Запись о явке не найдена"));
        attendanceRepository.delete(record);
    }

    private void requireUser(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("Пользователь не найден с id: " + userId);
        }
    }

    private static AttendanceRecordResponse toResponse(AttendanceRecord record) {
        return new AttendanceRecordResponse(record.getId(), record.getDate(), record.getEntry(), record.getExit());
    }
}
