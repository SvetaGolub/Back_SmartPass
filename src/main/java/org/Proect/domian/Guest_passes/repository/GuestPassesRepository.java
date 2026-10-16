package org.Proect.domian.Guest_passes.repository;

import org.Proect.domian.Guest_passes.model.GuestPasses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GuestPassesRepository extends JpaRepository<GuestPasses, Integer> {

    // Поиск пропуска по уникальному UID/ссылке (например, при сканировании на КПП)
    Optional<GuestPasses> findByUidLink(String uidLink);

    // Список всех пропусков, созданных конкретным пользователем (с пагинацией)
    @EntityGraph(attributePaths = {"createsByUserId"})
    Page<GuestPasses> findByCreatesByUserId_Id(Integer userId, Pageable pageable);

    // Поиск по имени гостя (частичное совпадение без учета регистра)
    List<GuestPasses> findByGuestNameContainingIgnoreCase(String guestName);

    // Поиск активных на данный момент пропусков (с учетом флага и временного окна)
    @Query("SELECT g FROM GuestPasses g WHERE g.isActivated = true " +
            "AND :now BETWEEN g.validFrom AND g.validTo")
    List<GuestPasses> findActivePassesAtTime(@Param("now") LocalDateTime now);


}
