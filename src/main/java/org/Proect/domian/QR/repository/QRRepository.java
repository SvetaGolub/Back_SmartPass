package org.Proect.domian.QR.repository;

import org.Proect.domian.QR.model.QR;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface QRRepository extends JpaRepository<QR, Integer> {

    // Поиск токена для валидации (использует уникальный индекс по token)
    Optional<QR> findByToken(String token);

    // Поиск актуального токена конкретного пользователя
    Optional<QR> findByUserId(Integer userId);

    // Проверка валидности токена в один запрос (не использован и не просрочен)
    @Query("SELECT q FROM QR q WHERE q.token = :token AND q.isUsed = false AND q.expiresAt > :now")
    Optional<QR> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    // Быстрая пометка токена как использованного (чтобы избежать повторного прохода)
    @Modifying
    @Transactional
    @Query("UPDATE QR q SET q.isUsed = true WHERE q.token = :token")
    int markAsUsed(@Param("token") String token);

    // Удаление просроченных или уже использованных токенов (для фонового планировщика/cron)
    @Modifying
    @Transactional
    @Query("DELETE FROM QR q WHERE q.expiresAt < :now OR q.isUsed = true")
    int deleteExpiredOrUsedTokens(@Param("now") LocalDateTime now);
}