package org.Proect.domian.Access_logs.repository;

import org.Proect.domian.Access_logs.model.AccessLogs;
import org.Proect.domian.Enums.AccessStatus;
import org.Proect.domian.Enums.Direction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccessLogsRepository extends JpaRepository<AccessLogs, String>{
    // Поиск логов конкретного пользователя с пагинацией (сортировку по scannedAt можно передать в Pageable)
    Page<AccessLogs> findByUserId(Integer userId, Pageable pageable);

    // Поиск логов по гостевому пропуску
    List<AccessLogs> findByGuestPassId(Integer guestPassId);

    // Поиск логов по точке доступа (чекпоинту)
    List<AccessLogs> findByCheckpointId(Integer checkpointId);

    // Поиск логов по статусу доступа (например, все отказы DENIED)
    List<AccessLogs> findByStatus(AccessStatus status);

    // Поиск логов за определенный период времени (использует индекс idx_scanned_at)
    List<AccessLogs> findByScannedAtBetween(LocalDateTime start, LocalDateTime end);

    // Комплексный поиск: история проходов через конкретный чекпоинт в определенном направлении
    List<AccessLogs> findByCheckpointIdAndDirection(Integer checkpointId, Direction direction);
}
