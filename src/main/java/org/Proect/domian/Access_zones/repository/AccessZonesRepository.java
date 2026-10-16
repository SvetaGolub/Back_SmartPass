package org.Proect.domian.Access_zones.repository;

import org.Proect.domian.Access_zones.model.AccessZones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AccessZonesRepository extends JpaRepository<AccessZones, Integer>{

    // Поиск зоны по уникальному имени
    Optional<AccessZones> findByName(String name);

    // Проверка существования зоны с таким именем (полезно при создании новой зоны)
    boolean existsByName(String name);

    // Быстрый поиск по части имени (например, для автокомплита в UI)
    java.util.List<AccessZones> findByNameContainingIgnoreCase(String name);
}

