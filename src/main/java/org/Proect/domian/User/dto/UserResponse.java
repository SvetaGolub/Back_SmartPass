package org.Proect.domian.User.dto;

import lombok.Data;
import org.Proect.domian.Enums.UserStatus;

@Data
public class UserResponse {
    private Integer id;
    private String roleName;
    private Integer zoneId;
    private String fullName;
    private String numberPhone;
    private String email;
    private String position;
    private String photoUrl;        // нужно охране на КПП
    private UserStatus status;
    private String blockingReason;
}

//record Responce(
//        Integer id,
//        String name
//) {
//
//}