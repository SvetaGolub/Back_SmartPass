package org.Proect.domian.Messages.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.Proect.domian.Messages.dto.MessageRequest;
import org.Proect.domian.Messages.dto.MessageResponse;
import org.Proect.domian.Messages.service.EmployeeMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/messages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeMessageController {

    private final EmployeeMessageService messageService;

    @GetMapping
    public List<MessageResponse> getMessages(@PathVariable Integer userId) {
        return messageService.getMessages(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(
            @PathVariable Integer userId,
            @Valid @RequestBody MessageRequest request,
            Authentication authentication) {
        return messageService.sendMessage(userId, authentication.getName(), request);
    }
}
