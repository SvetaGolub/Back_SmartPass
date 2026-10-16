package org.Proect.domian.User.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlockUserRequest {
    @NotBlank @Size(max = 255)
    private String reason;
}