package org.example.backend.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.event.ClassificationResultEvent;
import org.example.backend.service.ClassificationResultProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis Pub/Sub listener for AI classification results.
 * Subscribes to the classification results channel and publishes Spring events.
 * 
 * This aligns with the existing EDA pattern in the codebase:
 * - Uses ApplicationEventPublisher to publish events
 * - ClassificationResultProcessor listens via @EventListener
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "queue.enabled", havingValue = "true", matchIfMissing = true)
public class ClassificationResultListener implements MessageListener {

    private static final String RESULTS_CHANNEL = "classification:results";

    private final RedisConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ClassificationResultProcessor resultProcessor;

    private RedisMessageListenerContainer container;

    @PostConstruct
    public void init() {
        container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(this, new ChannelTopic(RESULTS_CHANNEL));
        container.afterPropertiesSet();
        container.start();

        log.info("ClassificationResultListener started - subscribed to channel: {}", RESULTS_CHANNEL);
    }

    @PreDestroy
    public void destroy() {
        if (container != null) {
            container.stop();
            log.info("ClassificationResultListener stopped");
        }
    }

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        try {
            String json = new String(message.getBody());
            log.debug("Received classification result from Redis channel");

            Map<String, Object> result = objectMapper.readValue(
                    json,
                    new TypeReference<Map<String, Object>>() {
                    });

            String jobId = (String) result.get("job_id");
            Boolean success = (Boolean) result.get("success");

            if (jobId == null) {
                log.warn("Received result without job_id - ignoring");
                return;
            }

            // Look up detection ID from pending jobs
            Long detectionId = resultProcessor.getDetectionIdForJob(jobId);
            if (detectionId == null) {
                log.debug("No pending detection for job {} - may be from external test", jobId);
                return;
            }

            log.info("Publishing ClassificationResultEvent for job: {} (detection: {}, success: {})",
                    jobId, detectionId, success);

            // Publish Spring event - aligns with existing EDA pattern
            eventPublisher.publishEvent(new ClassificationResultEvent(
                    this, jobId, detectionId, Boolean.TRUE.equals(success), result));

        } catch (Exception e) {
            log.error("Error processing classification result: {}", e.getMessage(), e);
        }
    }
}
