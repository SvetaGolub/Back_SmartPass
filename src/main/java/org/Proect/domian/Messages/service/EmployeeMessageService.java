package org.Proect.domian.Messages.service;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Messages.dto.MessageRequest;
import org.Proect.domian.Messages.dto.MessageResponse;
import org.Proect.domian.Messages.model.EmployeeMessage;
import org.Proect.domian.Messages.repository.EmployeeMessageRepository;
import org.Proect.domian.User.exception.UserNotFoundException;
import org.Proect.domian.User.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeMessageService {

    private final EmployeeMessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Integer employeeId) {
        requireEmployee(employeeId);
        return messageRepository.findByEmployeeIdOrderBySentAtAscIdAsc(employeeId)
                .stream()
                .map(EmployeeMessageService::toResponse)
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(Integer employeeId, String author, MessageRequest request) {
        requireEmployee(employeeId);
        EmployeeMessage message = new EmployeeMessage();
        message.setEmployeeId(employeeId);
        message.setAuthor(author);
        message.setText(request.text().trim());
        message.setSentAt(LocalDateTime.now());
        return toResponse(messageRepository.save(message));
    }

    private void requireEmployee(Integer employeeId) {
        if (!userRepository.existsById(employeeId)) {
            throw new UserNotFoundException("Пользователь не найден с id: " + employeeId);
        }
    }

    private static MessageResponse toResponse(EmployeeMessage message) {
        return new MessageResponse(message.getId(), message.getText(), message.getSentAt(), message.getAuthor());
    }
}
