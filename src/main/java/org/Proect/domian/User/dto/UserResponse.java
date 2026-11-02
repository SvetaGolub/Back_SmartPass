package org.Proect.domian.User.dto;

import lombok.Data;
import org.Proect.domian.Enums.UserStatus;

@Data
public class UserResponse {
    private Integer id;
    private Integer roleId;
    private String roleName;
    private Integer zoneId;
    private String firstName;
    private String lastName;
    private String middleName;
    private String fullName;
    private String numberPhone;
    private String email;
    private String position;
    private String address;
    private java.time.LocalDateTime birthday;
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