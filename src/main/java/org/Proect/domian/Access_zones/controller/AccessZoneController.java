package org.Proect.domian.Access_zones.controller;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Access_zones.dto.AccessZoneResponse;
import org.Proect.domian.Access_zones.repository.AccessZonesRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/access-zones")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SECURITY')")
public class AccessZoneController {

    private final AccessZonesRepository accessZonesRepository;

    @GetMapping
    public List<AccessZoneResponse> getAccessZones() {
        return accessZonesRepository.findAll(Sort.by("name")).stream()
                .map(zone -> new AccessZoneResponse(zone.getId(), zone.getName(), zone.getDescription()))
                .toList();
    }
}
