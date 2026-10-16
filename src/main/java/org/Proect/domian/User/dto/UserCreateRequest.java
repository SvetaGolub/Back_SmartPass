package org.Proect.domian.User.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class UserCreateRequest {
    @NotNull
    private Integer roleId;

    @NotNull
    private Integer zoneId;

    @NotBlank @Size(max = 50)
    private String firstName;

    @NotBlank @Size(max = 50)
    private String lastName;

    @Size(max = 50)
    private String middleName; // отчества может не быть

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

    @NotNull
    private LocalDateTime stage;

    @NotBlank @Size(min = 3, max = 50)
    private String login;

    @NotBlank @Size(min = 8, max = 72) // 72 байта: предел BCrypt
    private String password;
}