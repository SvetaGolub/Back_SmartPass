package org.Proect.domian.Guest_passes.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GuestPassCreateRequest {
    private Integer creatorUserId;     // ID сотрудника, который заказывает пропуск для гостя
    private String guestName;          // ФИО гостя
    private LocalDateTime validFrom;   // Дата и время начала действия
    private LocalDateTime validTo;     // Дата и время окончания действия

}