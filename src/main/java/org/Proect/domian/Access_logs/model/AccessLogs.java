package org.Proect.domian.Access_logs.model;

import lombok.*;
import jakarta.persistence.*;
import org.Proect.domian.Enums.AccessStatus;
import org.Proect.domian.Enums.Direction;
import org.Proect.domian.Guest_passes.model.GuestPasses;
import org.Proect.domian.Checkpoints.model.Checkpoints;
import org.Proect.domian.User.model.User;
import java.time.LocalDateTime;

@ToString
@NoArgsConstructor
@AllArgsConstructor

@Getter
@Setter
@Entity
@Table(name = "access_logs", indexes = {
        @Index(name = "idx_scanned_at", columnList = "scanned_at")
})

public class AccessLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkpoint_id", nullable = false)
    private Checkpoints checkpoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // NULLable для гостей
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_pass_id") // NULLable для штатных сотрудников
    private GuestPasses guestPass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessStatus status;

    @Column(name = "deny_reason", length = 255)
    private String denyReason;

    @Column(name = "scanned_at", insertable = false, updatable = false)
    private LocalDateTime  scannedAt;
}
