package org.Proect.domian.Access_logs.dto;

import lombok.Data;
import org.Proect.domian.Enums.Direction;

@Data
public class PassAttemptRequest {
    private Integer checkpointId;  // ID турникета/КПП
    private Direction direction;    // Направление: IN или OUT
    private String scannedCode;     // Считанный динамический QR или UID гостевого пропуска
}