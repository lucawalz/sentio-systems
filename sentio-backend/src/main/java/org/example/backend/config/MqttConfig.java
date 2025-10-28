package org.example.backend.config;

import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.example.backend.mqtt.AnimalDetectionHandler;
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
        options.setServerURIs(new String[]{mqttBroker});

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
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(mqttClientId,
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
            String topic = message.getHeaders().get("mqtt_receivedTopic").toString();
            String payload = message.getPayload().toString();

            System.out.println("=== MQTT MESSAGE RECEIVED ===");
            System.out.println("Topic: " + topic);
            System.out.println("Payload length: " + payload.length() + " characters");
            System.out.println("Timestamp: " + System.currentTimeMillis());
            System.out.println("=============================");

            try {
                // Route messages based on topic
                switch (topic) {
                    case "weather":
                        raspiWeatherDataHandler.processWeatherData(payload);
                        break;
                    case "animal_detection/events":  // New unified animal detection topic
                        animalDetectionHandler.processAnimalDetection(payload);
                        break;
                    case "camera":
                        // Handle other camera data if needed
                        System.out.println("Camera data received: " + payload.substring(0, Math.min(100, payload.length())) + "...");
                        break;
                    default:
                        System.out.println("Unknown topic: " + topic);
                        break;
                }
            } catch (Exception e) {
                System.err.println("Error processing MQTT message from topic " + topic + ": " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}