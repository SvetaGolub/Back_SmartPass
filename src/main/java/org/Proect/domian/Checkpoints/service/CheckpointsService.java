package org.Proect.domian.Checkpoints.service;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Access_zones.model.AccessZones;
import org.Proect.domian.Access_zones.repository.AccessZonesRepository;
import org.Proect.domian.Checkpoints.dto.CheckpointResponse;
import org.Proect.domian.Checkpoints.mapper.CheckpointMapper;
import org.Proect.domian.Checkpoints.model.Checkpoints;
import org.Proect.domian.Checkpoints.repository.CheckpointsRepository;
import org.Proect.domian.Enums.CheckpointStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckpointsService {

    private final CheckpointsRepository checkpointsRepository;
    private final AccessZonesRepository accessZonesRepository;
    private final CheckpointMapper checkpointMapper;

    @Transactional(readOnly = true)
    public List<CheckpointResponse> getAll() {
        return checkpointsRepository.findAll().stream()
                .map(checkpointMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CheckpointResponse getById(Integer id) {
        Checkpoints checkpoint = checkpointsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Чекпоинт не найден"));
        return checkpointMapper.toResponse(checkpoint);
    }

    @Transactional(readOnly = true)
    public List<CheckpointResponse> getCheckpointsByZone(Integer zoneId) {
        return checkpointsRepository.findByZoneId(zoneId).stream()
                .map(checkpointMapper::toResponse)
                .toList();
    }

    @Transactional
    public CheckpointResponse createCheckpoint(String name, Integer zoneId) {
        if (checkpointsRepository.existsByName(name)) {
            throw new IllegalStateException("Чекпоинт с именем '" + name + "' уже существует");
        }

        AccessZones zone = accessZonesRepository.findById(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("Указанная зона доступа не найдена"));

        Checkpoints checkpoint = new Checkpoints();
        checkpoint.setName(name);
        checkpoint.setZone(zone);
        checkpoint.setStatus(CheckpointStatus.ONLINE);

        return checkpointMapper.toResponse(checkpointsRepository.save(checkpoint));
    }

    @Transactional
    public CheckpointResponse updateStatus(Integer checkpointId, CheckpointStatus newStatus) {
        Checkpoints checkpoint = checkpointsRepository.findById(checkpointId)
                .orElseThrow(() -> new IllegalArgumentException("Чекпоинт не найден"));

        checkpoint.setStatus(newStatus);
        return checkpointMapper.toResponse(checkpointsRepository.save(checkpoint));
    }
}
