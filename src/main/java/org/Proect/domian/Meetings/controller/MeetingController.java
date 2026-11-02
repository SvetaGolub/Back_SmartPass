package org.Proect.domian.Meetings.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.Proect.domian.Meetings.dto.MeetingRequest;
import org.Proect.domian.Meetings.dto.MeetingResponse;
import org.Proect.domian.Meetings.service.MeetingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MeetingController {

    private final MeetingService meetingService;

    @GetMapping
    public List<MeetingResponse> getMeetings(@RequestParam int year, @RequestParam int month) {
        return meetingService.getForMonth(year, month);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingResponse createMeeting(@Valid @RequestBody MeetingRequest request) {
        return meetingService.create(request);
    }

    @PutMapping("/{id}")
    public MeetingResponse updateMeeting(@PathVariable long id, @Valid @RequestBody MeetingRequest request) {
        return meetingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeeting(@PathVariable long id) {
        meetingService.delete(id);
    }
}
