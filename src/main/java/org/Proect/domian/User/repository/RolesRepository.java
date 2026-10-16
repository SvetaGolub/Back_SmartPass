package org.Proect.domian.User.repository;

import org.Proect.domian.User.model.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RolesRepository extends JpaRepository<Roles, Integer> {

    Optional<Roles> findByName(String name);

    // Проверка существования роли (полезно при создании новых ролей, чтобы избежать дубликатов)
    boolean existsByName(String name);
}
