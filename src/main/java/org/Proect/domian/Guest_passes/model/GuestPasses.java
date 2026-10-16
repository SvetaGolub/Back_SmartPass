package org.Proect.domian.Guest_passes.model;

import lombok.*;
import jakarta.persistence.*;
import org.Proect.domian.User.model.User;

import java.time.LocalDateTime;

@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Getter
@Setter
@Entity
@Table(name = "guest_passes ")


public class GuestPasses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createsByUserId;

    @Column(name = "guest_name", nullable = false, length = 150)
    private String guestName;

    @Column(name = "uid_link", nullable = false, unique = true, length = 255)
    private String uidLink;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime  validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime  validTo;

    @Column(name = "is_activated", nullable = false)
    @Builder.Default
    private Boolean isActivated = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime  createdAt;
}
