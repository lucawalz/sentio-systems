package org.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
public class TestMqttConfig {
    @Bean(name = "mqttOutboundChannel")
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }
}
