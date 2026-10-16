package org.Proect.domian.QR.service;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.QR.dto.TokenResponse;
import org.Proect.domian.QR.mapper.QRMapper;
import org.Proect.domian.QR.model.QR;
import org.Proect.domian.QR.repository.QRRepository;
import org.Proect.domian.User.exception.UserNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final QRRepository qrRepository;

    //Сгенерировать или обновить динамический QR-токен для сотрудника.
    @Transactional
    public TokenResponse generateTokenForUser(Integer userId) {
        // Удаляем старый токен пользователя, если он был
        qrRepository.findByUserId(userId).ifPresent(qrRepository::delete);

        LocalDateTime now = LocalDateTime.now();

        QR newQr = QR.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString()) // Уникальная строка-токен
                .createdAt(now)
                .expiresAt(now.plusSeconds(30)) // Токен живет ровно 30 секунд
                .isUsed(false)
                .build();

        qrRepository.save(newQr);

        // Сразу возвращаем готовый DTO клиенту
        return QRMapper.toResponse(newQr);

    }

    // Получить информацию о текущем активном токене пользователя
    @Transactional(readOnly = true)
    public TokenResponse getTokenStatus(Integer userId) {
        QR qr = qrRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("У пользователя с id " + userId + " нет активных токенов"));

        // Проверяем, не истекло ли время действия токена
        boolean isExpired = qr.getExpiresAt().isBefore(LocalDateTime.now());

        if (isExpired || qr.getIsUsed()) {
            // Вместо обычной строки выбрасываем исключение, которое превратится в красивый JSON с ошибкой
            throw new IllegalArgumentException("Токен просрочен или уже использован");
        }

        return new TokenResponse(qr.getToken(), qr.getExpiresAt());
    }

    //Валидация токена при проходе (на турникете)
    @Transactional
    public QR validateAndUseToken(String token) {
        LocalDateTime now = LocalDateTime.now();

        QR qr = qrRepository.findValidToken(token, now)
                .orElseThrow(() -> new IllegalArgumentException("Токен недействителен, просрочен или уже использован"));

        // Гасим токен, чтобы по нему нельзя было пройти второй раз
        qrRepository.markAsUsed(token);

        return qr;
    }
}
