package org.Proect.domian.Attendance.repository;

import org.Proect.domian.Attendance.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByUserIdAndDateBetweenOrderByDateAsc(Integer userId, LocalDate start, LocalDate end);

    Optional<AttendanceRecord> findByUserIdAndDate(Integer userId, LocalDate date);

    List<AttendanceRecord> findByDateOrderByUserIdAsc(LocalDate date);
}
