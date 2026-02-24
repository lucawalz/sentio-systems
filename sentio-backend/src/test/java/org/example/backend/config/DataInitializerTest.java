package org.example.backend.config;

import org.example.backend.repository.AnimalDetectionRepository;
import org.example.backend.repository.DeviceRepository;
import org.example.backend.repository.RaspiWeatherDataRepository;
import org.example.backend.service.ViewerSessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataInitializer Unit Tests")
class DataInitializerTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private RaspiWeatherDataRepository weatherDataRepository;

    @Mock
    private AnimalDetectionRepository animalDetectionRepository;

    @Mock
    private ViewerSessionService viewerSessionService;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("Should initialize demo data when device repository is empty")
    void shouldInitializeData() {
        when(deviceRepository.count()).thenReturn(0L);

        dataInitializer.run(new DefaultApplicationArguments());

        verify(deviceRepository).saveAll(anyList());
        verify(weatherDataRepository).saveAll(anyList());
        verify(animalDetectionRepository).saveAll(anyList());
        verify(viewerSessionService).joinStream(eq("demo-device-001"), eq("demo-viewer-session-001"));
    }

    @Test
    @DisplayName("Should gracefully handle exceptions during viewer session join")
    void shouldHandleViewerSessionException() {
        when(deviceRepository.count()).thenReturn(0L);
        doThrow(new RuntimeException("Redis unavailable")).when(viewerSessionService).joinStream(anyString(),
                anyString());

        dataInitializer.run(new DefaultApplicationArguments());

        verify(deviceRepository).saveAll(anyList());
        verify(weatherDataRepository).saveAll(anyList());
        verify(animalDetectionRepository).saveAll(anyList());
        verify(viewerSessionService).joinStream(eq("demo-device-001"), eq("demo-viewer-session-001"));
        // Exception should be caught and logged, not propagated
    }

    @Test
    @DisplayName("Should skip initialization when demo data is already present")
    void shouldSkipInitialization() {
        when(deviceRepository.count()).thenReturn(1L);

        dataInitializer.run(new DefaultApplicationArguments());

        verify(deviceRepository, never()).saveAll(anyList());
        verify(weatherDataRepository, never()).saveAll(anyList());
        verify(animalDetectionRepository, never()).saveAll(anyList());
        verify(viewerSessionService, never()).joinStream(anyString(), anyString());
    }
}
