package org.example.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for submitting classification jobs to Redis queues.
 * Uses Event-Driven Architecture (EDA) - results are received via Redis
 * Pub/Sub.
 * 
 * Queue Protocol:
 * - Submit: LPUSH to {service}:queue:java with JSON job
 * - Result: Received via Redis Pub/Sub on classification:results channel
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisQueueService {

    private static final String SPECIESNET_QUEUE = "speciesnet:queue:java";
    private static final String BIRDER_QUEUE = "birder:queue:java";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Submit a classification job to the appropriate queue based on animal type.
     * 
     * @param imageBytes Raw image bytes
     * @param filename   Original filename
     * @param animalType Animal type (bird, mammal, etc.)
     * @return Job ID for result correlation
     */
    public String submitClassificationJob(byte[] imageBytes, String filename, String animalType) {
        String jobId = UUID.randomUUID().toString().replace("-", "");
        String queueName = getQueueForAnimalType(animalType);

        Map<String, Object> job = new HashMap<>();
        job.put("job_id", jobId);
        job.put("image_base64", Base64.getEncoder().encodeToString(imageBytes));
        job.put("filename", filename);
        job.put("animal_type", animalType);

        try {
            String jobJson = objectMapper.writeValueAsString(job);
            redisTemplate.opsForList().leftPush(queueName, jobJson);

            log.info("Submitted job {} to queue {} for {}", jobId, queueName, filename);
            return jobId;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize job: {}", e.getMessage());
            throw new RuntimeException("Failed to submit job to queue", e);
        }
    }

    /**
     * Submit a classification job asynchronously (EDA style).
     * Result will be received via Redis Pub/Sub and processed by
     * ClassificationResultProcessor via @EventListener.
     * 
     * @param imageBytes      Raw image bytes
     * @param filename        Original filename
     * @param animalType      Animal type
     * @param detectionId     Detection ID for result correlation
     * @param resultProcessor The processor that will handle the async result
     * @return Job ID
     */
    public String submitClassificationJobAsync(
            byte[] imageBytes,
            String filename,
            String animalType,
            Long detectionId,
            ClassificationResultProcessor resultProcessor) {

        String jobId = submitClassificationJob(imageBytes, filename, animalType);

        // Register job for async result processing
        resultProcessor.registerJob(jobId, detectionId);

        log.debug("Registered job {} for async result processing (detection: {})", jobId, detectionId);
        return jobId;
    }

    /**
     * Check if the queue service is available (Redis connected).
     */
    public boolean isAvailable() {
        try {
            return redisTemplate.getConnectionFactory() != null
                    && redisTemplate.getConnectionFactory().getConnection().ping() != null;
        } catch (Exception e) {
            log.warn("Redis queue not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get queue name based on animal type.
     */
    private String getQueueForAnimalType(String animalType) {
        if (animalType == null) {
            return SPECIESNET_QUEUE;
        }

        return switch (animalType.toLowerCase()) {
            case "bird" -> BIRDER_QUEUE;
            default -> SPECIESNET_QUEUE;
        };
    }
}
