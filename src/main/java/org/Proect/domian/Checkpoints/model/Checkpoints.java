package org.Proect.domian.Checkpoints.model;

import lombok.*;
import jakarta.persistence.*;
import org.Proect.domian.Enums.CheckpointStatus;
import org.Proect.domian.Access_zones.model.AccessZones;

@ToString

@Getter
@Setter
@Entity
@Table(name = "checkpoints")

public class Checkpoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", unique = true, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private AccessZones zone;

    @Enumerated(EnumType.STRING)
    private CheckpointStatus status = CheckpointStatus.ONLINE;
}
