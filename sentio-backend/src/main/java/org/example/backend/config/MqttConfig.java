package org.example.backend.config;

import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.example.backend.mqtt.AnimalDetectionHandler;
import org.example.backend.mqtt.DeviceStatusHandler;
import org.example.backend.mqtt.RaspiWeatherDataHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true")
public class MqttConfig {

    private final RaspiWeatherDataHandler raspiWeatherDataHandler;
    private final AnimalDetectionHandler animalDetectionHandler;
    private final DeviceStatusHandler deviceStatusHandler;

    @Value("${mqtt.broker}")
    private String mqttBroker;

    @Value("${mqtt.clientId}")
    private String mqttClientId;

    @Value("${mqtt.topics}")
    private String[] mqttTopics;

    // Authentication properties
    @Value("${mqtt.username:}")
    private String mqttUsername;

    @Value("${mqtt.password:}")
    private String mqttPassword;

    @Value("${mqtt.connectionTimeout:30}")
    private int connectionTimeout;

    @Value("${mqtt.keepAliveInterval:60}")
    private int keepAliveInterval;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();

        // Server URIs
        options.setServerURIs(new String[] { mqttBroker });

        // Authentication
        if (!mqttUsername.isEmpty() && !mqttPassword.isEmpty()) {
            options.setUserName(mqttUsername);
            options.setPassword(mqttPassword.toCharArray());
        }

        // Connection settings
        options.setCleanSession(true);
        options.setConnectionTimeout(connectionTimeout);
        options.setKeepAliveInterval(keepAliveInterval);
        options.setAutomaticReconnect(true);

        // Set will message for graceful disconnection
        options.setWill("status/backend", "Backend disconnected".getBytes(), 1, false);

        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer inboundChannel() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(mqttClientId,
                mqttClientFactory(),
                mqttTopics);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return message -> {
            Object topicHeader = message.getHeaders().get("mqtt_receivedTopic");
            if (topicHeader == null) {
                System.err.println("Received MQTT message without topic header");
                return;
            }
            String topic = topicHeader.toString();
            String payload = message.getPayload().toString();

            System.out.println("=== MQTT MESSAGE RECEIVED ===");
            System.out.println("Topic: " + topic);
            System.out.println("Payload length: " + payload.length() + " characters");
            System.out.println("Timestamp: " + System.currentTimeMillis());
            System.out.println("=============================");

            try {
                // Route messages based on topic
                if (topic.equals("weather/data") || topic.equals("weather")) {
                    raspiWeatherDataHandler.processWeatherData(payload);
                } else if (topic.equals("weather/status")) {
                    deviceStatusHandler.processStatusUpdate(payload);
                    System.out.println("Weather status: " + payload);
                } else if (topic.equals("animal_detection/events")) {
                    animalDetectionHandler.processAnimalDetection(payload);
                } else if (topic.equals("animal_detection/status") || topic.endsWith("/status")) {
                    deviceStatusHandler.processStatusUpdate(payload);
                    System.out.println("Device status: " + payload);
                } else if (topic.equals("camera")) {
                    System.out.println(
                            "Camera data received: " + payload.substring(0, Math.min(100, payload.length())) + "...");
                } else {
                    System.out.println("Unknown topic: " + topic);
                }
            } catch (Exception e) {
                System.err.println("Error processing MQTT message from topic " + topic + ": " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    // --- Outbound channel for publishing to devices ---

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutboundHandler() {
        org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler handler = new org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler(
                mqttClientId + "-outbound", mqttClientFactory());
        handler.setAsync(true);
        handler.setDefaultQos(1);
        // Topic is set per-message via header
        return handler;
    }
}