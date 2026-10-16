package org.Proect.domian.Access_logs.controller;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Access_logs.dto.PassAttemptRequest;
import org.Proect.domian.Access_logs.service.AccessControlService;
import org.Proect.domian.Access_logs.service.AccessControlService.AccessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/access-check")
@RequiredArgsConstructor
public class AccessCheckController {

    private final AccessControlService accessControlService;

    /**
     * Эндпоинт для проверки возможности прохода через точку доступа (турникет).
     * Вызывается контроллером турникета при сканировании токена.
     */
    @PostMapping("/verify")
    public ResponseEntity<AccessResponse> verifyPassAttempt(@RequestBody PassAttemptRequest request) {

        // Валидация входных данных на уровне API
        if (request.getCheckpointId() == null || request.getDirection() == null || request.getScannedCode() == null) {
            return ResponseEntity.badRequest().body(
                    new AccessResponse(false, "Неверный формат запроса. Все поля (checkpointId, direction, scannedCode) обязательны.")
            );
        }

        // Передаем обработку в слой бизнес-логики
        AccessResponse response = accessControlService.processPassAttempt(
                request.getCheckpointId(),
                request.getDirection(),
                request.getScannedCode()
        );

        // Возвращаем результат. Логирование (успешное или отказ) уже произошло внутри сервиса.
        return ResponseEntity.ok(response);
    }
}