package org.Proect.domian.Enums;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode // Это критически важно для работы составного ключа!
public class AccessRightId implements Serializable {
    private Integer userId;
    private Integer zoneId;
}
