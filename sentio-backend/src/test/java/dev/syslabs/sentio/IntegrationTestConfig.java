package dev.syslabs.sentio;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * Test configuration providing mock beans for integration tests.
 * Substitutes production MQTT infrastructure with test stubs.
 * Configures @Async methods to run synchronously in tests.
 */
@TestConfiguration
@EnableAsync
public class IntegrationTestConfig implements AsyncConfigurer {

    /**
     * Mock MQTT outbound channel for tests.
     * Required by StreamService but messages are discarded in tests.
     */
    @Bean("mqttOutboundChannel")
    @Primary
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * Configure @Async methods to run synchronously in tests.
     * This ensures test transactions can see changes made by async methods.
     */
    @Override
    public Executor getAsyncExecutor() {
        return new SyncTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return null;
    }
}
