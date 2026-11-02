package org.Proect.domian.Guest_passes.service;

import org.Proect.domian.Guest_passes.dto.GuestPassCreateRequest;
import org.Proect.domian.Guest_passes.dto.GuestPassResponse;
import org.Proect.domian.Guest_passes.mapper.GuestPassMapper;
import org.Proect.domian.Guest_passes.model.GuestPasses;
import org.Proect.domian.Guest_passes.repository.GuestPassesRepository;
import org.Proect.domian.User.exception.UserNotFoundException;
import org.Proect.domian.User.model.User;
import org.Proect.domian.User.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestPassesServiceTest {

    @Mock
    private GuestPassesRepository guestPassesRepository;

    @Mock
    private UserRepository userRepository;

    private GuestPassesService service;

    @BeforeEach
    void setUp() {
        service = new GuestPassesService(guestPassesRepository, userRepository, new GuestPassMapper());
    }

    @Test
    void rejectsPassThatExpiresBeforeItStarts() {
        GuestPassCreateRequest request = request(
                LocalDateTime.parse("2026-11-02T12:00:00"),
                LocalDateTime.parse("2026-11-02T11:59:59"));
        when(userRepository.findById(7)).thenReturn(Optional.of(new User()));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> service.createGuestPass(request));

        assertEquals("Дата окончания не может быть раньше даты начала", error.getMessage());
        verify(guestPassesRepository, never()).save(any());
    }

    @Test
    void createsPassWithRequestedDetailsAndInactiveState() {
        LocalDateTime validFrom = LocalDateTime.parse("2026-11-02T12:00:00");
        LocalDateTime validTo = validFrom;
        GuestPassCreateRequest request = request(validFrom, validTo);
        User creator = new User();
        creator.setId(7);
        creator.setLastName("Иванов");
        creator.setFirstName("Иван");
        creator.setMiddleName("Иванович");
        when(userRepository.findById(7)).thenReturn(Optional.of(creator));
        when(guestPassesRepository.save(any(GuestPasses.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GuestPassResponse response = service.createGuestPass(request);

        ArgumentCaptor<GuestPasses> savedPass = ArgumentCaptor.forClass(GuestPasses.class);
        verify(guestPassesRepository).save(savedPass.capture());
        GuestPasses pass = savedPass.getValue();
        assertAll(
                () -> assertSame(creator, pass.getCreatesByUserId()),
                () -> assertEquals("Гость Тестовый", pass.getGuestName()),
                () -> assertEquals(validFrom, pass.getValidFrom()),
                () -> assertEquals(validTo, pass.getValidTo()),
                () -> assertFalse(pass.getIsActivated()),
                () -> assertNotNull(pass.getUidLink()),
                () -> assertEquals(creator.getId(), response.getCreatorUserId()),
                () -> assertEquals("Иванов Иван Иванович", response.getCreatorFullName()),
                () -> assertEquals(pass.getUidLink(), response.getUidLink()));
    }

    @Test
    void doesNotCreatePassWhenCreatorDoesNotExist() {
        when(userRepository.findById(7)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.createGuestPass(request(
                LocalDateTime.parse("2026-11-02T12:00:00"),
                LocalDateTime.parse("2026-11-02T13:00:00"))));

        verify(guestPassesRepository, never()).save(any());
    }

    private static GuestPassCreateRequest request(LocalDateTime validFrom, LocalDateTime validTo) {
        GuestPassCreateRequest request = new GuestPassCreateRequest();
        request.setCreatorUserId(7);
        request.setGuestName("Гость Тестовый");
        request.setValidFrom(validFrom);
        request.setValidTo(validTo);
        return request;
    }
}
