package org.example.backend.config;

import org.example.backend.mqtt.AnimalDetectionHandler;
import org.example.backend.mqtt.DeviceStatusHandler;
import org.example.backend.mqtt.RaspiWeatherDataHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MqttConfig Unit Tests")
class MqttConfigTest {

    @Mock
    private RaspiWeatherDataHandler raspiWeatherDataHandler;
    @Mock
    private AnimalDetectionHandler animalDetectionHandler;
    @Mock
    private DeviceStatusHandler deviceStatusHandler;
    @Mock
    private Environment environment;

    @InjectMocks
    private MqttConfig mqttConfig;

    @BeforeEach
    void setUp() {
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{});
        ReflectionTestUtils.setField(mqttConfig, "mqttBroker", "tcp://localhost:1883");
        ReflectionTestUtils.setField(mqttConfig, "mqttClientId", "test-client");
        ReflectionTestUtils.setField(mqttConfig, "mqttTopics", new String[] { "weather/data", "animals/data" });
        ReflectionTestUtils.setField(mqttConfig, "connectionTimeout", 30);
        ReflectionTestUtils.setField(mqttConfig, "keepAliveInterval", 60);
    }

    @Test
    @DisplayName("Should create MqttPahoClientFactory with authentication")
    void shouldCreateMqttClientFactory() {
        ReflectionTestUtils.setField(mqttConfig, "mqttUsername", "user");
        ReflectionTestUtils.setField(mqttConfig, "mqttPassword", "pass");

        MqttPahoClientFactory factory = mqttConfig.mqttClientFactory();
        assertThat(factory).isNotNull();
        assertThat(factory.getConnectionOptions().getUserName()).isEqualTo("user");
        assertThat(factory.getConnectionOptions().getPassword()).isEqualTo("pass".toCharArray());
    }

    @Test
    @DisplayName("Should create MqttPahoClientFactory without authentication")
    void shouldCreateMqttClientFactoryWithoutAuth() {
        ReflectionTestUtils.setField(mqttConfig, "mqttUsername", "");
        ReflectionTestUtils.setField(mqttConfig, "mqttPassword", "");

        MqttPahoClientFactory factory = mqttConfig.mqttClientFactory();
        assertThat(factory).isNotNull();
        assertThat(factory.getConnectionOptions().getUserName()).isNull();
    }

    @Test
    @DisplayName("Should create input and outbound channels")
    void shouldCreateChannels() {
        MessageChannel inputChannel = mqttConfig.mqttInputChannel();
        MessageChannel outboundChannel = mqttConfig.mqttOutboundChannel();

        assertThat(inputChannel).isNotNull();
        assertThat(outboundChannel).isNotNull();
    }

    @Test
    @DisplayName("Should create inbound adapter securely")
    void shouldCreateInboundAdapter() {
        ReflectionTestUtils.setField(mqttConfig, "mqttUsername", "");
        ReflectionTestUtils.setField(mqttConfig, "mqttPassword", "");

        MqttPahoMessageDrivenChannelAdapter adapter = (MqttPahoMessageDrivenChannelAdapter) mqttConfig.inboundChannel();
        assertThat(adapter).isNotNull();
    }

    @Test
    @DisplayName("Should create outbound handler")
    void shouldCreateOutboundHandler() {
        ReflectionTestUtils.setField(mqttConfig, "mqttUsername", "");
        ReflectionTestUtils.setField(mqttConfig, "mqttPassword", "");

        MessageHandler handler = mqttConfig.mqttOutboundHandler();
        assertThat(handler).isInstanceOf(MqttPahoMessageHandler.class);
    }

    @Test
    @DisplayName("Should route messages correctly in inbound handler")
    void shouldRouteMessagesInHandler() throws Exception {
        MessageHandler handler = mqttConfig.handler();

        // Topic: weather/data
        Message<String> weatherMsg = new GenericMessage<>("{temp:20}", Map.of("mqtt_receivedTopic", "weather/data"));
        handler.handleMessage(weatherMsg);
        verify(raspiWeatherDataHandler).processWeatherData("{temp:20}");

        // Topic: animals/data
        Message<String> animalMsg = new GenericMessage<>("{species:'fox'}",
                Map.of("mqtt_receivedTopic", "animals/data"));
        handler.handleMessage(animalMsg);
        verify(animalDetectionHandler).processAnimalDetection("{species:'fox'}");

        // Topic: device/123/status
        Message<String> statusMsg = new GenericMessage<>("online", Map.of("mqtt_receivedTopic", "device/123/status"));
        handler.handleMessage(statusMsg);
        verify(deviceStatusHandler).processStatusUpdate("online");

        // Topic: unknown (should just log and not throw)
        Message<String> unknownMsg = new GenericMessage<>("payload", Map.of("mqtt_receivedTopic", "unknown/topic"));
        handler.handleMessage(unknownMsg);

        // Missing topic header
        Message<String> noTopicMsg = new GenericMessage<>("payload");
        handler.handleMessage(noTopicMsg);
    }

    @Test
    @DisplayName("Handler should catch exceptions")
    void handlerShouldCatchExceptions() throws Exception {
        MessageHandler handler = mqttConfig.handler();
        Message<String> weatherMsg = new GenericMessage<>("{temp:20}", Map.of("mqtt_receivedTopic", "weather/data"));

        doThrow(new RuntimeException("Parsing failure")).when(raspiWeatherDataHandler).processWeatherData(anyString());

        // This should not throw outward as the handler catches any exception
        handler.handleMessage(weatherMsg);
    }
}
