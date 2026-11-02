package org.Proect.domian.Meetings.repository;

import org.Proect.domian.Meetings.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByDateBetweenOrderByDateAscTimeAscIdAsc(LocalDate start, LocalDate end);
}
