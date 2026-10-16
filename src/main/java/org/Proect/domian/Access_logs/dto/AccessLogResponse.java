package org.Proect.domian.Access_logs.dto;

import lombok.Data;
import org.Proect.domian.Enums.AccessStatus;
import org.Proect.domian.Enums.Direction;
import java.time.LocalDateTime;

@Data
public class AccessLogResponse {
    private Integer id;
    private Integer checkpointId;
    private String checkpointName;
    private String zoneName;
    private Integer userId;
    private String userFullName;      // Для сотрудников
    private Integer guestPassId;
    private String guestName;         // Для гостей
    private Direction direction;
    private AccessStatus status;
    private String denyReason;
    private LocalDateTime scannedAt;
}