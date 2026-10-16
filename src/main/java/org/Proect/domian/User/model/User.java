package org.Proect.domian.User.model;

import lombok.*;
import jakarta.persistence.*;
import org.Proect.domian.Enums.UserStatus;

import java.sql.Date;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name =  "role_id", nullable = false)
    private Roles role;

    @Column(name = "zone_id", nullable = false)
    private Integer zoneId;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "middle_name", nullable = false, length = 50)
    private String middleName;

    @Column(name = "number_phone", nullable = false, unique = true, length = 30)
    private String numberPhone;

    @Column(nullable = false, unique = true, length = 30)
    private String email;

    @Column(nullable = false, length = 100)
    private String position;

    @Column(nullable = false, length = 100)
    private String address;

    @Column(nullable = false)
    private LocalDateTime birthday;

    @Column(nullable = false)
    private LocalDateTime stage;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @Column(nullable = false, unique = true, length = 50)
    private String login ;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private UserStatus  status = UserStatus.ACTIVE;

    @Column(name = "blocking_reason", length = 255)
    private String blockingReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime  createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;


}
