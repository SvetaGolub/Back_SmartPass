package org.Proect.domian.Guest_passes.service;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Guest_passes.dto.GuestPassCreateRequest;
import org.Proect.domian.Guest_passes.dto.GuestPassResponse;
import org.Proect.domian.Guest_passes.mapper.GuestPassMapper;
import org.Proect.domian.Guest_passes.model.GuestPasses;
import org.Proect.domian.Guest_passes.repository.GuestPassesRepository;
import org.Proect.domian.User.exception.UserNotFoundException;
import org.Proect.domian.User.model.User;
import org.Proect.domian.User.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuestPassesService {

    private final GuestPassesRepository guestPassesRepository;
    private final UserRepository userRepository;
    private final GuestPassMapper guestPassMapper;

    @Transactional
    public GuestPassResponse createGuestPass(GuestPassCreateRequest request) {
        User creator = userRepository.findById(request.getCreatorUserId())
                .orElseThrow(() -> new UserNotFoundException("Сотрудник, создающий пропуск, не найден с id: " + request.getCreatorUserId()));

        if (request.getValidTo().isBefore(request.getValidFrom())) {
            throw new IllegalArgumentException("Дата окончания не может быть раньше даты начала");
        }

        // Красивый и современный Lombok Builder вместо сеттеров
        GuestPasses pass = GuestPasses.builder()
                .createsByUserId(creator)
                .guestName(request.getGuestName())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .isActivated(false)
                .uidLink(UUID.randomUUID().toString())
                .build();

        return guestPassMapper.toResponse(guestPassesRepository.save(pass));
    }

    @Transactional
    public void activatePass(Integer passId) {
        GuestPasses pass = guestPassesRepository.findById(passId)
                .orElseThrow(() -> new UserNotFoundException("Пропуск не найден с id: " + passId));
        pass.setIsActivated(true);
    }

    @Transactional(readOnly = true)
    public GuestPassResponse getPassByUid(String uid) {
        return guestPassesRepository.findByUidLink(uid)
                .map(guestPassMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException("Пропуск с UID " + uid + " не найден"));
    }

    @Transactional(readOnly = true)
    public List<GuestPassResponse> searchByGuestName(String name) {
        return guestPassesRepository.findByGuestNameContainingIgnoreCase(name).stream()
                .map(guestPassMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<GuestPassResponse> getPassesByCreator(Integer userId, Pageable pageable) {
        return guestPassesRepository.findByCreatesByUserId_Id(userId, pageable)
                .map(guestPassMapper::toResponse);
    }
}
