package org.Proect.domian.Checkpoints.dto;

import lombok.Data;
import org.Proect.domian.Enums.CheckpointStatus;

@Data
public class CheckpointResponse {
    private Integer id;
    private String name;
    private Integer zoneId; // Вместо тяжелой сущности передаем плоский ID зоны
    private String zoneName;
    private CheckpointStatus status;
}
