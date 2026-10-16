package org.Proect.domian.User.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.Proect.domian.User.dto.BlockUserRequest;
import org.Proect.domian.User.dto.UserCreateRequest;
import org.Proect.domian.User.dto.UserResponse;
import org.Proect.domian.User.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

// Имена ролей (ADMIN, SECURITY) должны совпадать со значениями в таблице roles
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORT_FIELDS =
            Set.of("id", "lastName", "firstName", "position", "status");

    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY')")
    public UserResponse getUserById(@PathVariable Integer id) {
        return userService.getUserById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY')")
    public Page<UserResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastName") String sortBy) {

        if (!SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Сортировка по полю '" + sortBy + "' не поддерживается");
        }
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), clampSize(size), Sort.by(sortBy).ascending());
        return userService.getAllUsers(pageable);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY')")
    public Page<UserResponse> searchUsers(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        return userService.searchUsers(name, pageable);
    }

    @GetMapping("/zone/{zoneId}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY')")
    public List<UserResponse> getUsersByZone(@PathVariable Integer zoneId) {
        return userService.getUsersByZone(zoneId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest request) {
        return userService.registerUser(request);
    }

    @PutMapping("/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY')")
    public void blockUser(@PathVariable Integer id, @Valid @RequestBody BlockUserRequest request) {
        userService.blockUser(id, request.getReason());
    }

    @PutMapping("/{id}/unblock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void unblockUser(@PathVariable Integer id) {
        userService.unblockUser(id);
    }

    private static int clampSize(int size) {
        return Math.clamp(size, 1, MAX_PAGE_SIZE);
    }
}