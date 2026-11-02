package org.Proect.domian.User.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserUpdateRequest {

    @NotNull
    private Integer roleId;

    @NotNull
    private Integer zoneId;

    @NotBlank @Size(max = 50)
    private String firstName;

    @NotBlank @Size(max = 50)
    private String lastName;

    @Size(max = 50)
    private String middleName;

    @NotBlank @Size(max = 30)
    private String numberPhone;

    @NotBlank @Email @Size(max = 100)
    private String email;

    @NotBlank @Size(max = 100)
    private String position;

    @NotBlank @Size(max = 100)
    private String address;

    @NotNull @Past
    private LocalDateTime birthday;
}
