package org.Proect.domian.Meetings.controller;

import org.Proect.config.SecurityConfig;
import org.Proect.domian.Meetings.dto.MeetingResponse;
import org.Proect.domian.Meetings.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingController.class)
@Import(SecurityConfig.class)
class MeetingControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MeetingService meetingService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void allowsAdminToCreateMeeting() throws Exception {
        when(meetingService.create(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MeetingResponse(1L, "Review", LocalDate.of(2026, 11, 3),
                        LocalTime.of(10, 0), null));

        mockMvc.perform(post("/api/v1/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Review","date":"2026-11-03","time":"10:00","note":""}
                                """))
                .andExpect(status().isCreated());

        verify(meetingService).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(roles = "SECURITY")
    void deniesSecurityRoleFromCreatingMeeting() throws Exception {
        mockMvc.perform(post("/api/v1/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Review","date":"2026-11-03","time":"10:00","note":""}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(meetingService);
    }
}
