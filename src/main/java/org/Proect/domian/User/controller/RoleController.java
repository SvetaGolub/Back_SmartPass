package org.Proect.domian.User.controller;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.User.dto.RoleResponse;
import org.Proect.domian.User.repository.RolesRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SECURITY')")
public class RoleController {

    private final RolesRepository rolesRepository;

    @GetMapping
    public List<RoleResponse> getRoles() {
        return rolesRepository.findAll(Sort.by("name")).stream()
                .map(role -> new RoleResponse(role.getId(), role.getName(), role.getDescription()))
                .toList();
    }
}
