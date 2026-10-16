package org.Proect.domian.QR.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TokenResponse {
    private String token;             // Сам сгенерированный токен (строка UUID)
    private LocalDateTime expiresAt;  // Время, до которого токен будет валиден (30 секунд)
}