package org.Proect.domian.Guest_passes.controller;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Guest_passes.dto.GuestPassCreateRequest;
import org.Proect.domian.Guest_passes.dto.GuestPassResponse;
import org.Proect.domian.Guest_passes.service.GuestPassesService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/guest-passes")
@RequiredArgsConstructor
public class GuestPassesController {

    private final GuestPassesService guestPassesService;

    // 1. Создать новую заявку на гостевой пропуск
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuestPassResponse createPass(@RequestBody GuestPassCreateRequest request) {
        return guestPassesService.createGuestPass(request);
    }

    // 2. Активировать пропуск охраны на КПП
    @PutMapping("/{id}/activate")
    public void activatePass(@PathVariable Integer id) {
        guestPassesService.activatePass(id);
    }

    // 3. Поиск пропуска по уникальному UID / ссылке (для сканеров)
    @GetMapping("/search/uid")
    public GuestPassResponse getPassByUid(@RequestParam String uid) {
        return guestPassesService.getPassByUid(uid);
    }

    // 4. Поиск пропусков по имени гостя (для интерфейса охраны)
    @GetMapping("/search/name")
    public List<GuestPassResponse> searchByGuestName(@RequestParam String name) {
        return guestPassesService.searchByGuestName(name);
    }

    // 5. Получить все пропуска конкретного сотрудника (с пагинацией)
    @GetMapping("/user/{userId}")
    public Page<GuestPassResponse> getPassesByCreator(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return guestPassesService.getPassesByCreator(userId, pageable);
    }
}
