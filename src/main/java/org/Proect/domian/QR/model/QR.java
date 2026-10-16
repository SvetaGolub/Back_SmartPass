package org.Proect.domian.QR.model;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@ToString

@Getter
@Setter
@Entity
@Table(name = "dynamic_tokens", indexes = {
        @Index(name = "idx_expires", columnList = "expires_at"),
        @Index(name = "idx_user_token", columnList = "user_id, token")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class QR {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId;

    @Column(name = "token", unique = true, length = 255)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime  expiresAt;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime  createdAt;
}
