package org.Proect.domian.Access_zones.service;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Access_zones.model.AccessZones;
import org.Proect.domian.Access_zones.repository.AccessZonesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccessZonesService {

    private final AccessZonesRepository accessZonesRepository;

    @Transactional(readOnly = true)
    public List<AccessZones> getAllZones() {
        return accessZonesRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AccessZones getZoneById(Integer id) {
        return accessZonesRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Зона доступа с ID " + id + " не найдена"));
    }

    @Transactional
    public AccessZones createZone(String name, String description) {
        // Бизнес-правило: имя зоны должно быть уникальным
        if (accessZonesRepository.existsByName(name)) {
            throw new IllegalStateException("Зона доступа с именем '" + name + "' уже существует");
        }

        AccessZones zone = new AccessZones();
        zone.setName(name);
        zone.setDescription(description);

        return accessZonesRepository.save(zone);
    }
}
