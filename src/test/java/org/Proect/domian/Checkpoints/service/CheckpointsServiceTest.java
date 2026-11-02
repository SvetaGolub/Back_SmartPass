package org.Proect.domian.Checkpoints.service;

import org.Proect.domian.Access_zones.model.AccessZones;
import org.Proect.domian.Access_zones.repository.AccessZonesRepository;
import org.Proect.domian.Checkpoints.dto.CheckpointCreateRequest;
import org.Proect.domian.Checkpoints.mapper.CheckpointMapper;
import org.Proect.domian.Checkpoints.model.Checkpoints;
import org.Proect.domian.Checkpoints.repository.CheckpointsRepository;
import org.Proect.domian.Enums.CheckpointStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckpointsServiceTest {

    @Mock
    private CheckpointsRepository checkpointsRepository;

    @Mock
    private AccessZonesRepository accessZonesRepository;

    private CheckpointsService service;
    private static Validator validator;
    private static ValidatorFactory validatorFactory;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
        validator = null;
        validatorFactory = null;
    }

    @BeforeEach
    void setUp() {
        service = new CheckpointsService(checkpointsRepository, accessZonesRepository, new CheckpointMapper());
    }

    @Test
    void checkpointCreateRequestRequiresNameAndZone() {
        CheckpointCreateRequest request = new CheckpointCreateRequest();
        request.setName("  ");

        var violations = validator.validate(request);

        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("zoneId")));
    }

    @Test
    void rejectsDuplicateCheckpointNameWithoutSaving() {
        when(checkpointsRepository.existsByName("Main gate")).thenReturn(true);

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> service.createCheckpoint("Main gate", 3));

        assertTrue(error.getMessage().contains("Main gate"));
        verify(accessZonesRepository, never()).findById(any());
        verify(checkpointsRepository, never()).save(any());
    }

    @Test
    void rejectsCheckpointWhenAccessZoneDoesNotExist() {
        when(checkpointsRepository.existsByName("Main gate")).thenReturn(false);
        when(accessZonesRepository.findById(3)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> service.createCheckpoint("Main gate", 3));

        assertEquals("Указанная зона доступа не найдена", error.getMessage());
        verify(checkpointsRepository, never()).save(any());
    }

    @Test
    void createsCheckpointOnlineAndReturnsItsZone() {
        AccessZones zone = new AccessZones();
        zone.setId(3);
        zone.setName("Main zone");
        when(checkpointsRepository.existsByName("Main gate")).thenReturn(false);
        when(accessZonesRepository.findById(3)).thenReturn(Optional.of(zone));
        when(checkpointsRepository.save(any(Checkpoints.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createCheckpoint("Main gate", 3);

        assertAll(
                () -> assertEquals("Main gate", response.getName()),
                () -> assertEquals(3, response.getZoneId()),
                () -> assertEquals("Main zone", response.getZoneName()),
                () -> assertEquals(CheckpointStatus.ONLINE, response.getStatus()));
        verify(checkpointsRepository).save(argThat(checkpoint ->
                checkpoint.getZone() == zone && checkpoint.getStatus() == CheckpointStatus.ONLINE));
    }
}
