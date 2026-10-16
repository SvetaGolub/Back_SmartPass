package org.Proect.domian.Checkpoints.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckpointCreateRequest {
    @NotBlank(message = "Имя чекпоинта не может быть пустым")
    private String name;

    @NotNull(message = "ID зоны доступа обязателен")
    private Integer zoneId;
}
