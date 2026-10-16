package org.Proect.domian.Access_logs.controller;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Access_logs.dto.AccessLogResponse;
import org.Proect.domian.Access_logs.model.AccessLogs;
import org.Proect.domian.Access_logs.repository.AccessLogsRepository;
import org.Proect.domian.Enums.AccessStatus;
import org.Proect.domian.Enums.Direction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/access-logs")
@RequiredArgsConstructor
public class AccessLogsController {

    private final AccessLogsRepository accessLogsRepository;

    // 1. Получить всю историю проходов с пагинацией (по умолчанию сортировка от новых к старым)
    @GetMapping
    public ResponseEntity<Page<AccessLogResponse>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "scannedAt") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<AccessLogs> logsPage = accessLogsRepository.findAll(pageable);

        return ResponseEntity.ok(logsPage.map(this::convertToResponse));
    }

    // 2. Получить логи конкретного сотрудника (с пагинацией)
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AccessLogResponse>> getLogsByUser(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<AccessLogs> logsPage = accessLogsRepository.findByUserId(userId, pageable);

        return ResponseEntity.ok(logsPage.map(this::convertToResponse));
    }

    // 3. Получить логи по конкретной точке доступа (чекпоинту)
    @GetMapping("/checkpoint/{checkpointId}")
    public ResponseEntity<List<AccessLogResponse>> getLogsByCheckpoint(@PathVariable Integer checkpointId) {
        List<AccessLogs> logs = accessLogsRepository.findByCheckpointId(checkpointId);
        return ResponseEntity.ok(logs.stream().map(this::convertToResponse).toList());
    }

    // 4. Поиск инцидентов / отказов в доступе (например, статус DENIED)
    @GetMapping("/status")
    public ResponseEntity<List<AccessLogResponse>> getLogsByStatus(@RequestParam AccessStatus status) {
        List<AccessLogs> logs = accessLogsRepository.findByStatus(status);
        return ResponseEntity.ok(logs.stream().map(this::convertToResponse).toList());
    }

    // 5. Фильтрация логов за определенный период времени (Использует индекс idx_scanned_at)
    @GetMapping("/time-range")
    public ResponseEntity<List<AccessLogResponse>> getLogsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        List<AccessLogs> logs = accessLogsRepository.findByScannedAtBetween(start, end);
        return ResponseEntity.ok(logs.stream().map(this::convertToResponse).toList());
    }

    // Вспомогательный метод-маппер
    private AccessLogResponse convertToResponse(AccessLogs log) {
        AccessLogResponse dto = new AccessLogResponse();
        dto.setId(log.getId());
        dto.setDirection(log.getDirection());
        dto.setStatus(log.getStatus());
        dto.setDenyReason(log.getDenyReason());
        dto.setScannedAt(log.getScannedAt());

        // Безопасный маппинг чекпоинта и зоны
        if (log.getCheckpoint() != null) {
            dto.setCheckpointId(log.getCheckpoint().getId());
            dto.setCheckpointName(log.getCheckpoint().getName());
            if (log.getCheckpoint().getZone() != null) {
                dto.setZoneName(log.getCheckpoint().getZone().getName());
            }
        }

        // Если пробовал пройти штатный сотрудник
        if (log.getUser() != null) {
            dto.setUserId(log.getUser().getId());
            dto.setUserFullName(String.format("%s %s %s",
                    log.getUser().getLastName(),
                    log.getUser().getFirstName(),
                    log.getUser().getMiddleName()).trim());
        }

        // Если пробовал пройти гость
        if (log.getGuestPass() != null) {
            dto.setGuestPassId(log.getGuestPass().getId());
            dto.setGuestName(log.getGuestPass().getGuestName());
        }

        return dto;
    }
}