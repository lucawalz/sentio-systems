package org.example.backend.service;

import org.example.backend.model.Device;
import org.example.backend.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoHeartbeatServiceTest {

    @Mock
    private MessageChannel mqttOutboundChannel;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DemoHeartbeatService demoHeartbeatService;

    @Captor
    private ArgumentCaptor<Message<String>> messageCaptor;

    private Device device1;
    private Device device2;

    @BeforeEach
    void setUp() {
        device1 = new Device();
        device1.setId("demo-device-001");
        device1.setLatitude(48.2485);
        device1.setLongitude(11.6525);

        device2 = new Device();
        device2.setId("demo-device-002");
        device2.setLatitude(48.0890);
        device2.setLongitude(11.4724);
    }

    @Test
    void sendHeartbeats_shouldPublishMqttMessages_whenDevicesExistWithCoordinates() {
        // Arrange
        when(deviceRepository.findById("demo-device-001")).thenReturn(Optional.of(device1));
        when(deviceRepository.findById("demo-device-002")).thenReturn(Optional.of(device2));
        when(mqttOutboundChannel.send(any(Message.class))).thenReturn(true);

        // Act
        demoHeartbeatService.sendHeartbeats();

        // Assert
        verify(mqttOutboundChannel, times(2)).send(messageCaptor.capture());

        var messages = messageCaptor.getAllValues();

        // Assert Device 1 payload
        Message<String> msg1 = messages.get(0);
        assertThat(msg1.getHeaders().get("mqtt_topic")).isEqualTo("device/demo-device-001/status");
        assertThat(msg1.getPayload()).contains("\"device_id\":\"demo-device-001\"");
        assertThat(msg1.getPayload()).contains("\"ip\":\"192.168.1.101\"");
        assertThat(msg1.getPayload()).contains("\"service\":\"weather\"");
        assertThat(msg1.getPayload()).contains("\"latitude\":48.248500");
        assertThat(msg1.getPayload()).contains("\"longitude\":11.652500");

        // Assert Device 2 payload
        Message<String> msg2 = messages.get(1);
        assertThat(msg2.getHeaders().get("mqtt_topic")).isEqualTo("device/demo-device-002/status");
        assertThat(msg2.getPayload()).contains("\"device_id\":\"demo-device-002\"");
        assertThat(msg2.getPayload()).contains("\"ip\":\"192.168.1.102\"");
        assertThat(msg2.getPayload()).contains("\"service\":\"camera\"");
        assertThat(msg2.getPayload()).contains("\"latitude\":48.089000");
        assertThat(msg2.getPayload()).contains("\"longitude\":11.472400");
    }

    @Test
    void sendHeartbeats_shouldSkip_whenDeviceNotFound() {
        // Arrange
        when(deviceRepository.findById("demo-device-001")).thenReturn(Optional.empty());
        when(deviceRepository.findById("demo-device-002")).thenReturn(Optional.empty());

        // Act
        demoHeartbeatService.sendHeartbeats();

        // Assert
        verify(mqttOutboundChannel, never()).send(any(Message.class));
    }

    @Test
    void sendHeartbeats_shouldSkip_whenDeviceHasNoCoordinates() {
        // Arrange
        device1.setLatitude(null); // Remove coordinates
        when(deviceRepository.findById("demo-device-001")).thenReturn(Optional.of(device1));
        when(deviceRepository.findById("demo-device-002")).thenReturn(Optional.empty());

        // Act
        demoHeartbeatService.sendHeartbeats();

        // Assert
        verify(mqttOutboundChannel, never()).send(any(Message.class));
    }

    @Test
    void sendHeartbeats_shouldCatchException_whenMqttFails() {
        // Arrange
        when(deviceRepository.findById("demo-device-001")).thenReturn(Optional.of(device1));
        when(deviceRepository.findById("demo-device-002")).thenReturn(Optional.of(device2));

        when(mqttOutboundChannel.send(any(Message.class)))
                .thenThrow(new RuntimeException("MQTT Exception"))
                .thenReturn(true);

        // Act
        demoHeartbeatService.sendHeartbeats();

        // Assert
        verify(mqttOutboundChannel, times(2)).send(any(Message.class));
    }
}
