package org.Proect.domian.Access_logs.service;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Access_logs.model.AccessLogs;
import org.Proect.domian.Access_logs.repository.AccessLogsRepository;
import org.Proect.domian.Checkpoints.model.Checkpoints;
import org.Proect.domian.Checkpoints.repository.CheckpointsRepository;
import org.Proect.domian.Enums.AccessStatus;
import org.Proect.domian.Enums.CheckpointStatus;
import org.Proect.domian.Enums.Direction;
import org.Proect.domian.Enums.UserStatus;
import org.Proect.domian.Guest_passes.model.GuestPasses;
import org.Proect.domian.Guest_passes.repository.GuestPassesRepository;
import org.Proect.domian.QR.model.QR;
import org.Proect.domian.QR.service.TokenService;
import org.Proect.domian.User.model.User;
import org.Proect.domian.User.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final CheckpointsRepository checkpointsRepository;
    private final UserRepository userRepository;
    private final GuestPassesRepository guestPassesRepository;
    private final AccessLogsRepository accessLogsRepository;
    private final TokenService tokenService;

    /**
     * Главная точка входа: обработка попытки прохода.
     * Принимает идентификатор турникета, направление движения и считанный токен (QR или UID гостя).
     */
    @Transactional
    public AccessResponse processPassAttempt(Integer checkpointId, Direction direction, String scannedCode) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Проверяем существование и статус чекпоинта
        Checkpoints checkpoint = checkpointsRepository.findById(checkpointId)
                .orElseThrow(() -> new IllegalArgumentException("Чекпоинт не найден"));

        if (checkpoint.getStatus() != CheckpointStatus.ONLINE) {
            return logAndDeny(checkpoint, null, null, direction, "Чекпоинт отключен или находится на обслуживании");
        }

        // 2. Определяем, кто пытается пройти — сотрудник по QR или гость по ссылке/UID
        // Сначала пробуем найти как гостевой пропуск
        Optional<GuestPasses> guestPassOpt = guestPassesRepository.findByUidLink(scannedCode);
        if (guestPassOpt.isPresent()) {
            return processGuestPass(checkpoint, direction, guestPassOpt.get(), now);
        }

        // Если не гость, пробуем обработать как динамический QR сотрудника
        try {
            QR qrToken = tokenService.validateAndUseToken(scannedCode); // Метод сам погасит QR, если он валиден
            User user = userRepository.findById(qrToken.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь, привязанный к токену, не найден"));

            return processUserAccess(checkpoint, direction, user);
        } catch (IllegalArgumentException e) {
            // Сюда попадем, если токен не валиден, просрочен или не найден вообще
            return logAndDeny(checkpoint, null, null, direction, "Неверный, использованный или просроченный идентификатор: " + e.getMessage());
        }
    }

    // Бизнес-логика проверки прав сотрудника
    private AccessResponse processUserAccess(Checkpoints checkpoint, Direction direction, User user) {
        // Проверяем, активен ли сотрудник
        if (user.getStatus() != UserStatus.ACTIVE) {
            return logAndDeny(checkpoint, user, null, direction, "Пользователь заблокирован. Причина: " + user.getBlockingReason());
        }

        // Проверяем уровень доступа: совпадает ли домашняя зона пользователя с зоной чекпоинта
        // Примечание: в вашей сущности User поле zone — это Integer (id зоны)
        if (!user.getZoneId().equals(checkpoint.getZone().getId())) {
            return logAndDeny(checkpoint, user, null, direction,
                    String.format("Нет прав доступа в зону '%s'. Разрешенная зона ID: %d", checkpoint.getZone().getName(), user.getZoneId()));
        }

        // Если все проверки пройдены — разрешаем проход
        return logAndAllow(checkpoint, user, null, direction);
    }

    // Бизнес-логика проверки гостевого пропуска
    private AccessResponse processGuestPass(Checkpoints checkpoint, Direction direction, GuestPasses pass, LocalDateTime now) {
        // Проверяем, активирован ли пропуск администрацией/охраной
        if (Boolean.FALSE.equals(pass.getIsActivated())) {
            return logAndDeny(checkpoint, null, pass, direction, "Гостевой пропуск еще не активирован");
        }

        // Проверяем временное окно действия пропуска
        if (now.isBefore(pass.getValidFrom())) {
            return logAndDeny(checkpoint, null, pass, direction, "Срок действия пропуска еще не начался");
        }
        if (now.isAfter(pass.getValidTo())) {
            return logAndDeny(checkpoint, null, pass, direction, "Срок действия пропуска истек");
        }

        // Если все ок — разрешаем проход гостю
        return logAndAllow(checkpoint, null, pass, direction);
    }

    // Вспомогательный метод для сохранения лога ОТКАЗА
    private AccessResponse logAndDeny(Checkpoints checkpoint, User user, GuestPasses pass, Direction direction, String reason) {
        AccessLogs log = createBaseLog(checkpoint, user, pass, direction);
        log.setStatus(AccessStatus.DENIED);
        log.setDenyReason(reason);
        accessLogsRepository.save(log);

        return new AccessResponse(false, reason);
    }

    // Вспомогательный метод для сохранения лога РАЗРЕШЕНИЯ прохода
    private AccessResponse logAndAllow(Checkpoints checkpoint, User user, GuestPasses pass, Direction direction) {
        AccessLogs log = createBaseLog(checkpoint, user, pass, direction);
        log.setStatus(AccessStatus.ALLOWED);
        accessLogsRepository.save(log);

        return new AccessResponse(true, "Доступ разрешен");
    }

    // Сборка базового объекта лога
    private AccessLogs createBaseLog(Checkpoints checkpoint, User user, GuestPasses pass, Direction direction) {
        AccessLogs log = new AccessLogs();
        log.setCheckpoint(checkpoint);
        log.setUser(user);
        log.setGuestPass(pass);
        log.setDirection(direction);
        // Поле scannedAt не заполняем вручную, так как в сущности прописано insertable = false (заполнится на стороне БД)
        return log;
    }

    // DTO (Data Transfer Object) для ответа контроллеру/контроллеру турникета
    public record AccessResponse(boolean isAllowed, String message) {}
}
