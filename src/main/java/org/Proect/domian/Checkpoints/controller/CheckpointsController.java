package org.Proect.domian.Checkpoints.controller;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Checkpoints.dto.CheckpointCreateRequest;
import org.Proect.domian.Checkpoints.dto.CheckpointResponse;
import org.Proect.domian.Checkpoints.service.CheckpointsService;
import org.Proect.domian.Enums.CheckpointStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/checkpoints")
@RequiredArgsConstructor
public class CheckpointsController {

    private final CheckpointsService checkpointsService;

    @GetMapping
    public ResponseEntity<List<CheckpointResponse>> getAllCheckpoints() {
        return ResponseEntity.ok(checkpointsService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CheckpointResponse> getCheckpointById(@PathVariable Integer id) {
        return ResponseEntity.ok(checkpointsService.getById(id));
    }

    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<List<CheckpointResponse>> getCheckpointsByZone(@PathVariable Integer zoneId) {
        return ResponseEntity.ok(checkpointsService.getCheckpointsByZone(zoneId));
    }

    @PostMapping
    public ResponseEntity<CheckpointResponse> createCheckpoint(
            @Valid @RequestBody CheckpointCreateRequest request) {
        CheckpointResponse created = checkpointsService.createCheckpoint(request.getName(), request.getZoneId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CheckpointResponse> updateCheckpointStatus(
            @PathVariable Integer id,
            @RequestParam CheckpointStatus status) {
        return ResponseEntity.ok(checkpointsService.updateStatus(id, status));
    }
}
