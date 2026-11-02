package org.Proect.domian.Messages.service;

import org.Proect.domian.Messages.dto.MessageRequest;
import org.Proect.domian.Messages.dto.MessageResponse;
import org.Proect.domian.Messages.model.EmployeeMessage;
import org.Proect.domian.Messages.repository.EmployeeMessageRepository;
import org.Proect.domian.User.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeMessageServiceTest {

    @Mock
    private EmployeeMessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    private EmployeeMessageService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeMessageService(messageRepository, userRepository);
    }

    @Test
    void sendsTrimmedMessageWithAuthenticatedAuthorAndTimestamp() {
        when(userRepository.existsById(7)).thenReturn(true);
        when(messageRepository.save(any(EmployeeMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MessageRequest request = new MessageRequest("  Проверьте расписание  ");

        MessageResponse response = service.sendMessage(7, "admin.login", request);

        ArgumentCaptor<EmployeeMessage> saved = ArgumentCaptor.forClass(EmployeeMessage.class);
        verify(messageRepository).save(saved.capture());
        assertAll(
                () -> assertEquals(7, saved.getValue().getEmployeeId()),
                () -> assertEquals("admin.login", saved.getValue().getAuthor()),
                () -> assertEquals("Проверьте расписание", saved.getValue().getText()),
                () -> assertNotNull(saved.getValue().getSentAt()),
                () -> assertEquals("admin.login", response.author()),
                () -> assertEquals("Проверьте расписание", response.text()));
    }

    @Test
    void returnsMessagesOrderedByRepositoryForExistingUser() {
        when(userRepository.existsById(7)).thenReturn(true);
        EmployeeMessage message = new EmployeeMessage();
        message.setId(2L);
        message.setEmployeeId(7);
        message.setAuthor("admin.login");
        message.setText("Сообщение");
        message.setSentAt(LocalDateTime.parse("2026-11-02T10:00:00"));
        when(messageRepository.findByEmployeeIdOrderBySentAtAscIdAsc(7)).thenReturn(List.of(message));

        List<MessageResponse> result = service.getMessages(7);

        assertEquals(List.of(new MessageResponse(
                2L, "Сообщение", LocalDateTime.parse("2026-11-02T10:00:00"), "admin.login")), result);
    }

    @Test
    void doesNotReadOrSendMessagesForUnknownUser() {
        when(userRepository.existsById(77)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> service.getMessages(77));
        assertThrows(RuntimeException.class,
                () -> service.sendMessage(77, "admin.login", new MessageRequest("Текст")));
        verifyNoInteractions(messageRepository);
    }
}
