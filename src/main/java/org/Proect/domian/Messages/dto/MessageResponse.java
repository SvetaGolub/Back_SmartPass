package org.Proect.domian.Messages.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        String text,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime sentAt,
        String author) {
}
