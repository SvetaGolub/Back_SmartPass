package org.Proect.domian.Meetings.service;

import org.Proect.domian.Meetings.dto.MeetingRequest;
import org.Proect.domian.Meetings.dto.MeetingResponse;
import org.Proect.domian.Meetings.model.Meeting;
import org.Proect.domian.Meetings.repository.MeetingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    private MeetingService service;

    @BeforeEach
    void setUp() {
        service = new MeetingService(meetingRepository);
    }

    @Test
    void loadsMeetingsWithinRequestedMonthInRepositoryOrder() {
        Meeting meeting = meeting(4L, "Планёрка", LocalDate.of(2026, 11, 3), LocalTime.of(10, 0), null);
        when(meetingRepository.findByDateBetweenOrderByDateAscTimeAscIdAsc(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .thenReturn(List.of(meeting));

        List<MeetingResponse> result = service.getForMonth(2026, 11);

        assertEquals(1, result.size());
        assertEquals(new MeetingResponse(4L, "Планёрка", LocalDate.of(2026, 11, 3), LocalTime.of(10, 0), null),
                result.getFirst());
    }

    @Test
    void trimsMeetingTextOnCreateAndPreservesOptionalNote() {
        MeetingRequest request = request("  Планёрка  ", "2026-11-03", "10:00", "  Краткая повестка  ");
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> {
            Meeting meeting = invocation.getArgument(0);
            meeting.setId(5L);
            return meeting;
        });

        MeetingResponse response = service.create(request);

        ArgumentCaptor<Meeting> saved = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository).save(saved.capture());
        assertAll(
                () -> assertEquals("Планёрка", saved.getValue().getTitle()),
                () -> assertEquals("Краткая повестка", saved.getValue().getNote()),
                () -> assertEquals(5L, response.id()),
                () -> assertEquals(LocalDate.of(2026, 11, 3), response.date()),
                () -> assertEquals(LocalTime.of(10, 0), response.time()));
    }

    @Test
    void rejectsUpdateForMissingMeeting() {
        when(meetingRepository.findById(90L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.update(90L, request("Планёрка", "2026-11-03", "10:00", null)));
        verify(meetingRepository, never()).save(any());
    }

    @Test
    void rejectsDeleteForMissingMeeting() {
        when(meetingRepository.existsById(90L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.delete(90L));
        verify(meetingRepository, never()).deleteById(anyLong());
    }

    private static Meeting meeting(long id, String title, LocalDate date, LocalTime time, String note) {
        Meeting meeting = new Meeting();
        meeting.setId(id);
        meeting.setTitle(title);
        meeting.setDate(date);
        meeting.setTime(time);
        meeting.setNote(note);
        return meeting;
    }

    private static MeetingRequest request(String title, String date, String time, String note) {
        MeetingRequest request = new MeetingRequest();
        request.setTitle(title);
        request.setDate(LocalDate.parse(date));
        request.setTime(LocalTime.parse(time));
        request.setNote(note);
        return request;
    }
}
