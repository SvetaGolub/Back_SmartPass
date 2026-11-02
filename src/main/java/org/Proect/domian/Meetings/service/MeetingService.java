package org.Proect.domian.Meetings.service;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Meetings.dto.MeetingRequest;
import org.Proect.domian.Meetings.dto.MeetingResponse;
import org.Proect.domian.Meetings.model.Meeting;
import org.Proect.domian.Meetings.repository.MeetingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;

    @Transactional(readOnly = true)
    public List<MeetingResponse> getForMonth(int year, int month) {
        YearMonth requestedMonth = YearMonth.of(year, month);
        return meetingRepository.findByDateBetweenOrderByDateAscTimeAscIdAsc(
                        requestedMonth.atDay(1), requestedMonth.atEndOfMonth())
                .stream()
                .map(MeetingService::toResponse)
                .toList();
    }

    @Transactional
    public MeetingResponse create(MeetingRequest request) {
        Meeting meeting = new Meeting();
        apply(meeting, request);
        return toResponse(meetingRepository.save(meeting));
    }

    @Transactional
    public MeetingResponse update(long id, MeetingRequest request) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Собрание не найдено"));
        apply(meeting, request);
        return toResponse(meetingRepository.save(meeting));
    }

    @Transactional
    public void delete(long id) {
        if (!meetingRepository.existsById(id)) {
            throw new IllegalArgumentException("Собрание не найдено");
        }
        meetingRepository.deleteById(id);
    }

    private static void apply(Meeting meeting, MeetingRequest request) {
        meeting.setTitle(request.getTitle().trim());
        meeting.setDate(request.getDate());
        meeting.setTime(request.getTime());
        meeting.setNote(request.getNote() == null || request.getNote().isBlank()
                ? null : request.getNote().trim());
    }

    private static MeetingResponse toResponse(Meeting meeting) {
        return new MeetingResponse(meeting.getId(), meeting.getTitle(), meeting.getDate(),
                meeting.getTime(), meeting.getNote());
    }
}
