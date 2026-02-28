package org.example.backend.service;

import org.example.backend.event.AnimalDetectedEvent;
import org.example.backend.model.AnimalDetection;
import org.example.backend.repository.AnimalDetectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnimalDetectionCommandService Unit Tests")
class AnimalDetectionCommandServiceTest {

    @Mock
    private AnimalDetectionRepository animalDetectionRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private DeviceService deviceService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AnimalDetectionCommandService commandService;

    @Test
    @DisplayName("Should save animal detection and publish event")
    void shouldSaveAnimalDetection() {
        AnimalDetection detection = new AnimalDetection();
        detection.setAnimalType("bird");
        detection.setSpecies("Robin");
        detection.setConfidence(0.9f);

        AnimalDetection savedDetection = new AnimalDetection();
        savedDetection.setId(1L);
        savedDetection.setAnimalType("bird");
        savedDetection.setSpecies("Robin");

        when(animalDetectionRepository.save(detection)).thenReturn(savedDetection);

        AnimalDetection result = commandService.saveAnimalDetection(detection);

        assertThat(result.getId()).isEqualTo(1L);
        verify(animalDetectionRepository).save(detection);
        verify(eventPublisher).publishEvent(any(AnimalDetectedEvent.class));
    }

    @Test
    @DisplayName("Should return false when deleting non-existent detection")
    void shouldReturnFalseForNonExistentDetection() {
        when(animalDetectionRepository.findById(1L)).thenReturn(Optional.empty());

        boolean result = commandService.deleteDetection(1L);

        assertThat(result).isFalse();
        verify(animalDetectionRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should throw exception when attempting to delete without device access")
    void shouldThrowWhenNoAccess() {
        AnimalDetection detection = new AnimalDetection();
        detection.setId(1L);
        detection.setDeviceId("device-1");

        when(animalDetectionRepository.findById(1L)).thenReturn(Optional.of(detection));
        when(deviceService.hasAccessToDevice("device-1")).thenReturn(false);

        assertThatThrownBy(() -> commandService.deleteDetection(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(animalDetectionRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should delete detection without image")
    void shouldDeleteDetectionWithoutImage() {
        AnimalDetection detection = new AnimalDetection();
        detection.setId(1L);
        detection.setDeviceId("device-1");
        detection.setImageUrl(null);

        when(animalDetectionRepository.findById(1L)).thenReturn(Optional.of(detection));
        when(deviceService.hasAccessToDevice("device-1")).thenReturn(true);

        boolean result = commandService.deleteDetection(1L);

        assertThat(result).isTrue();
        verify(imageStorageService, never()).deleteImage(anyString());
        verify(animalDetectionRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should delete detection with image")
    void shouldDeleteDetectionWithImage() {
        AnimalDetection detection = new AnimalDetection();
        detection.setId(1L);
        detection.setDeviceId("device-1");
        detection.setImageUrl("images/test.jpg");

        when(animalDetectionRepository.findById(1L)).thenReturn(Optional.of(detection));
        when(deviceService.hasAccessToDevice("device-1")).thenReturn(true);
        when(imageStorageService.deleteImage("images/test.jpg")).thenReturn(true);

        boolean result = commandService.deleteDetection(1L);

        assertThat(result).isTrue();
        verify(imageStorageService).deleteImage("images/test.jpg");
        verify(animalDetectionRepository).deleteById(1L);
    }
}
