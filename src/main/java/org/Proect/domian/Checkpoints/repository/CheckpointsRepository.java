package org.Proect.domian.Checkpoints.repository;

import org.Proect.domian.Checkpoints.model.Checkpoints;
import org.Proect.domian.Enums.CheckpointStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckpointsRepository extends JpaRepository<Checkpoints, Integer> {
    Optional<Checkpoints> findByName(String name);

    boolean existsByName(String name);

    List<Checkpoints> findByStatus(CheckpointStatus status);

    @EntityGraph(attributePaths = {"zone"})
    List<Checkpoints> findByZoneId(Integer zoneId);

    @EntityGraph(attributePaths = {"zone"})
    Optional<Checkpoints> findById(Integer id);

    @EntityGraph(attributePaths = {"zone"})
    List<Checkpoints> findAll();
}
