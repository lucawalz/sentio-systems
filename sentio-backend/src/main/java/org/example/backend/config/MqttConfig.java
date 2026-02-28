package org.example.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.example.backend.mqtt.AnimalDetectionHandler;
import org.example.backend.mqtt.DeviceStatusHandler;
import org.example.backend.mqtt.RaspiWeatherDataHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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
@Slf4j
public class MqttConfig {

    private final RaspiWeatherDataHandler raspiWeatherDataHandler;
    private final AnimalDetectionHandler animalDetectionHandler;
    private final DeviceStatusHandler deviceStatusHandler;
    private final Environment environment;

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

    // TLS/SSL properties
    @Value("${mqtt.tls.enabled:false}")
    private boolean tlsEnabled;

    @Value("${mqtt.tls.ca-cert-path:}")
    private String tlsCaCertPath;

    @Value("${mqtt.tls.client-cert-path:}")
    private String tlsClientCertPath;

    @Value("${mqtt.tls.client-key-path:}")
    private String tlsClientKeyPath;

    @Value("${mqtt.tls.client-key-password:}")
    private String tlsClientKeyPassword;

    @Value("${mqtt.tls.verify-hostname:true}")
    private boolean tlsVerifyHostname;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();

        boolean productionProfileActive = java.util.Arrays.stream(environment.getActiveProfiles())
                .map(String::toLowerCase)
                .anyMatch(profile -> profile.equals("prod") || profile.equals("production"));

        if (productionProfileActive && !tlsEnabled) {
            throw new IllegalStateException("MQTT TLS must be enabled in production profiles");
        }

        if (tlsEnabled && mqttBroker.startsWith("tcp://")) {
            throw new IllegalStateException("MQTT broker URL must use ssl:// or mqtts:// when TLS is enabled");
        }

        // Server URIs
        options.setServerURIs(new String[] { mqttBroker });

        // Authentication
        if (!mqttUsername.isEmpty() && !mqttPassword.isEmpty()) {
            options.setUserName(mqttUsername);
            options.setPassword(mqttPassword.toCharArray());
        }

        // TLS/SSL Configuration
        if (tlsEnabled) {
            try {
                java.util.Properties sslProperties = new java.util.Properties();
                
                // CA certificate for server verification
                if (!tlsCaCertPath.isEmpty()) {
                    sslProperties.setProperty("com.ibm.ssl.trustStore", tlsCaCertPath);
                    if (!tlsClientKeyPassword.isEmpty()) {
                        sslProperties.setProperty("com.ibm.ssl.trustStorePassword", tlsClientKeyPassword);
                    }
                }
                
                // Client certificate for mutual TLS authentication
                if (!tlsClientCertPath.isEmpty()) {
                    sslProperties.setProperty("com.ibm.ssl.keyStore", tlsClientCertPath);
                    if (!tlsClientKeyPassword.isEmpty()) {
                        sslProperties.setProperty("com.ibm.ssl.keyStorePassword", tlsClientKeyPassword);
                    }
                }
                
                options.setSSLProperties(sslProperties);
                options.setSSLHostnameVerifier(tlsVerifyHostname 
                    ? null  // null = default hostname verifier (strict)
                    : (hostname, session) -> true);  // accept all (development only!)

                log.info("MQTT TLS/SSL enabled (hostname verification: {})", tlsVerifyHostname);
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure MQTT TLS/SSL", e);
            }
        } else {
            log.warn("MQTT TLS/SSL is disabled. This is insecure for production.");
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
                log.warn("Received MQTT message without topic header");
                return;
            }
            String topic = topicHeader.toString();
            String payload = message.getPayload().toString();

            log.debug("MQTT message received (topic={}, payloadLength={})", topic, payload.length());

            try {
                // Route messages based on topic
                if (topic.equals("weather/data")) {
                    raspiWeatherDataHandler.processWeatherData(payload);
                } else if (topic.equals("animals/data")) {
                    animalDetectionHandler.processAnimalDetection(payload);
                } else if (topic.startsWith("device/") && topic.endsWith("/status")) {
                    // Unified device status: device/{deviceId}/status
                    deviceStatusHandler.processStatusUpdate(payload);
                    log.debug("Processed device status MQTT message");
                } else if (topic.equals("camera")) {
                    log.debug("Processed camera MQTT message");
                } else {
                    log.debug("Unhandled MQTT topic: {}", topic);
                }
            } catch (Exception e) {
                log.error("Error processing MQTT message from topic {}", topic, e);
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