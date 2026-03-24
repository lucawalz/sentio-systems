package dev.syslabs.sentio.config;

import dev.syslabs.sentio.model.Device;
import dev.syslabs.sentio.repository.AnimalDetectionRepository;
import dev.syslabs.sentio.repository.DeviceRepository;
import dev.syslabs.sentio.repository.RaspiWeatherDataRepository;
import dev.syslabs.sentio.service.ViewerSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private RaspiWeatherDataRepository weatherDataRepository;
    @Mock
    private AnimalDetectionRepository animalDetectionRepository;
    @Mock
    private ViewerSessionService viewerSessionService;
    @Mock
    private Keycloak keycloak;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RealmResource realmResource;
    @Mock
    private UsersResource usersResource;

    @InjectMocks
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dataInitializer, "realm", "test-realm");
    }

    @Test
    void run_shouldSkip_whenDevicesExist() {
        when(deviceRepository.count()).thenReturn(1L);

        dataInitializer.run(null);

        verify(deviceRepository, never()).saveAll(any());
    }

    @Test
    void run_shouldInitializeData_whenDevicesDoNotExistAndUserExists() {
        when(deviceRepository.count()).thenReturn(0L);

        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        UserRepresentation existingUser = new UserRepresentation();
        existingUser.setId("user-123");
        when(usersResource.search("demo", true)).thenReturn(List.of(existingUser));

        dataInitializer.run(null);

        verify(deviceRepository, times(1)).saveAll(anyList());
        verify(weatherDataRepository, times(1)).saveAll(anyList());
        verify(animalDetectionRepository, times(1)).saveAll(anyList());
        verify(viewerSessionService, times(1)).joinStream(eq("demo-device-001"), eq("demo-viewer-session-001"));
        verify(eventPublisher, times(2)).publishEvent(any());
    }

    @Test
    void run_shouldInitializeData_whenDevicesDoNotExistAndUserDoesNotExist() throws Exception {
        when(deviceRepository.count()).thenReturn(0L);

        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        when(usersResource.search("demo", true)).thenReturn(Collections.emptyList());

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation())
                .thenReturn(new URI("http://localhost/auth/admin/realms/test-realm/users/user-456"));
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

        dataInitializer.run(null);

        verify(usersResource, times(1)).create(any(UserRepresentation.class));
        verify(deviceRepository, times(1)).saveAll(anyList());
        verify(weatherDataRepository, times(1)).saveAll(anyList());
        verify(animalDetectionRepository, times(1)).saveAll(anyList());
        verify(viewerSessionService, times(1)).joinStream(eq("demo-device-001"), eq("demo-viewer-session-001"));
        verify(eventPublisher, times(2)).publishEvent(any());
    }

    @Test
    void run_shouldAbortSilently_whenKeycloakUserCreationFails() {
        when(deviceRepository.count()).thenReturn(0L);

        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        when(usersResource.search("demo", true)).thenReturn(Collections.emptyList());

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

        // run() catches Keycloak failures and returns early without throwing
        dataInitializer.run(null);

        verify(deviceRepository, never()).saveAll(anyList());
    }

    @Test
    void run_shouldHandleViewerSessionException() {
        when(deviceRepository.count()).thenReturn(0L);

        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        UserRepresentation existingUser = new UserRepresentation();
        existingUser.setId("user-123");
        when(usersResource.search("demo", true)).thenReturn(List.of(existingUser));

        doThrow(new RuntimeException("Redis error")).when(viewerSessionService).joinStream(anyString(), anyString());

        dataInitializer.run(null); // Should not throw

        verify(viewerSessionService, times(1)).joinStream(eq("demo-device-001"), eq("demo-viewer-session-001"));
    }
}
