package org.Proect.domian.Guest_passes.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GuestPassResponse {
    private Integer id;
    private Integer creatorUserId;
    private String creatorFullName;
    private String guestName;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean isActivated;
    private String uidLink;
}
