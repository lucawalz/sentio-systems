package org.example.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for {@link RedisQueueService}.
 * Uses real Redis Testcontainer to validate queue operations with production
 * parity.
 */
class RedisQueueServiceTest extends BaseIntegrationTest {

    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    static {
        redis.start();
    }

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RedisQueueService redisQueueService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SPECIESNET_QUEUE = "speciesnet:queue:java";
    private static final String BIRDER_QUEUE = "birder:queue:java";
    private static final byte[] TEST_IMAGE = "fake-image-data".getBytes(StandardCharsets.UTF_8);
    private static final String TEST_FILENAME = "test.jpg";

    @BeforeEach
    void setUp() {
        // Clean up Redis queues before each test
        Set<String> keys = redisTemplate.keys("*:queue:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Nested
    @DisplayName("Job Submission Tests")
    class JobSubmissionTests {

        @Test
        @DisplayName("should submit classification job successfully to queue")
        void shouldSubmitJobSuccessfully() throws Exception {
            // When
            String jobId = redisQueueService.submitClassificationJob(TEST_IMAGE, TEST_FILENAME, "mammal");

            // Then
            assertThat(jobId).isNotBlank();
            assertThat(jobId).hasSize(32); // UUID without hyphens

            // Verify job in Redis queue
            String jobJson = redisTemplate.opsForList().rightPop(SPECIESNET_QUEUE);
            assertThat(jobJson).isNotNull();

            Map<String, Object> job = objectMapper.readValue(jobJson, new TypeReference<>() {
            });
            assertThat(job).containsEntry("job_id", jobId);
            assertThat(job).containsEntry("filename", TEST_FILENAME);
            assertThat(job).containsEntry("animal_type", "mammal");
            assertThat(job).containsKey("image_base64");

            // Verify image is correctly encoded
            String imageBase64 = (String) job.get("image_base64");
            byte[] decodedImage = Base64.getDecoder().decode(imageBase64);
            assertThat(decodedImage).isEqualTo(TEST_IMAGE);
        }

        @Test
        @DisplayName("should generate unique job IDs for each submission")
        void shouldGenerateUniqueJobIds() {
            // When
            String jobId1 = redisQueueService.submitClassificationJob(TEST_IMAGE, "image1.jpg", "bird");
            String jobId2 = redisQueueService.submitClassificationJob(TEST_IMAGE, "image2.jpg", "bird");
            String jobId3 = redisQueueService.submitClassificationJob(TEST_IMAGE, "image3.jpg", "mammal");

            // Then
            assertThat(jobId1).isNotEqualTo(jobId2);
            assertThat(jobId1).isNotEqualTo(jobId3);
            assertThat(jobId2).isNotEqualTo(jobId3);

            // All should have valid format (32-char hex without hyphens)
            assertThat(jobId1).matches("[a-f0-9]{32}");
            assertThat(jobId2).matches("[a-f0-9]{32}");
            assertThat(jobId3).matches("[a-f0-9]{32}");
        }

        @Test
        @DisplayName("should use correct queue key format")
        void shouldUseCorrectQueueKeyFormat() {
            // When
            redisQueueService.submitClassificationJob(TEST_IMAGE, TEST_FILENAME, "mammal");
            redisQueueService.submitClassificationJob(TEST_IMAGE, TEST_FILENAME, "bird");

            // Then
            Long speciesnetSize = redisTemplate.opsForList().size(SPECIESNET_QUEUE);
            Long birderSize = redisTemplate.opsForList().size(BIRDER_QUEUE);

            assertThat(speciesnetSize).isEqualTo(1);
            assertThat(birderSize).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle multiple jobs to same queue")
        void shouldHandleMultipleJobsToSameQueue() {
            // When - submit 5 jobs to the same queue
            String jobId1 = redisQueueService.submitClassificationJob(TEST_IMAGE, "image1.jpg", "mammal");
            String jobId2 = redisQueueService.submitClassificationJob(TEST_IMAGE, "image2.jpg", "mammal");
            String jobId3 = redisQueueService.submitClassificationJob(TEST_IMAGE, "image3.jpg", "mammal");
            String jobId4 = redisQueueService.submitClassificationJob(TEST_IMAGE, "image4.jpg", "mammal");
            String jobId5 = redisQueueService.submitClassificationJob(TEST_IMAGE, "image5.jpg", "mammal");

            // Then - all jobs should be in queue
            Long queueSize = redisTemplate.opsForList().size(SPECIESNET_QUEUE);
            assertThat(queueSize).isEqualTo(5);

            // Jobs should be retrievable in FIFO order (leftPush/rightPop)
            List<String> allJobs = redisTemplate.opsForList().range(SPECIESNET_QUEUE, 0, -1);
            assertThat(allJobs).hasSize(5);
        }

        @Test
        @DisplayName("should handle large image data")
        void shouldHandleLargeImageData() throws Exception {
            // Given - 1MB image
            byte[] largeImage = new byte[1024 * 1024];
            for (int i = 0; i < largeImage.length; i++) {
                largeImage[i] = (byte) (i % 256);
            }

            // When
            String jobId = redisQueueService.submitClassificationJob(largeImage, "large.jpg", "bird");

            // Then
            assertThat(jobId).isNotBlank();

            String jobJson = redisTemplate.opsForList().rightPop(BIRDER_QUEUE);
            Map<String, Object> job = objectMapper.readValue(jobJson, new TypeReference<>() {
            });

            String imageBase64 = (String) job.get("image_base64");
            byte[] decodedImage = Base64.getDecoder().decode(imageBase64);
            assertThat(decodedImage).hasSize(largeImage.length);
        }

        @Test
        @DisplayName("should handle special characters in filename")
        void shouldHandleSpecialCharactersInFilename() throws Exception {
            // Given
            String specialFilename = "test-image_2024 (1) [final].jpg";

            // When
            String jobId = redisQueueService.submitClassificationJob(TEST_IMAGE, specialFilename, "mammal");

            // Then
            assertThat(jobId).isNotBlank();

            String jobJson = redisTemplate.opsForList().rightPop(SPECIESNET_QUEUE);
            Map<String, Object> job = objectMapper.readValue(jobJson, new TypeReference<>() {
            });

            assertThat(job.get("filename")).isEqualTo(specialFilename);
        }
    }

    @Nested
    @DisplayName("Queue Routing Tests")
    class QueueRoutingTests {

        @Test
        @DisplayName("should route bird jobs to birder queue")
        void shouldRouteBirdJobsToBirderQueue() {
            // When
            redisQueueService.submitClassificationJob(TEST_IMAGE, "bird.jpg", "bird");

            // Then
            Long birderSize = redisTemplate.opsForList().size(BIRDER_QUEUE);
            Long speciesnetSize = redisTemplate.opsForList().size(SPECIESNET_QUEUE);

            assertThat(birderSize).isEqualTo(1);
            assertThat(speciesnetSize).isEqualTo(0);
        }

        @Test
        @DisplayName("should route mammal jobs to speciesnet queue")
        void shouldRouteMammalJobsToSpeciesnetQueue() {
            // When
            redisQueueService.submitClassificationJob(TEST_IMAGE, "mammal.jpg", "mammal");

            // Then
            Long speciesnetSize = redisTemplate.opsForList().size(SPECIESNET_QUEUE);
            Long birderSize = redisTemplate.opsForList().size(BIRDER_QUEUE);

            assertThat(speciesnetSize).isEqualTo(1);
            assertThat(birderSize).isEqualTo(0);
        }

        @Test
        @DisplayName("should route unknown animal types to default speciesnet queue")
        void shouldRouteUnknownToDefaultQueue() {
            // When
            redisQueueService.submitClassificationJob(TEST_IMAGE, "unknown.jpg", "reptile");
            redisQueueService.submitClassificationJob(TEST_IMAGE, "unknown2.jpg", "fish");
            redisQueueService.submitClassificationJob(TEST_IMAGE, "unknown3.jpg", "insect");

            // Then
            Long speciesnetSize = redisTemplate.opsForList().size(SPECIESNET_QUEUE);
            Long birderSize = redisTemplate.opsForList().size(BIRDER_QUEUE);

            assertThat(speciesnetSize).isEqualTo(3);
            assertThat(birderSize).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle case-insensitive animal type routing")
        void shouldHandleCaseInsensitiveRouting() {
            // When
            redisQueueService.submitClassificationJob(TEST_IMAGE, "bird1.jpg", "BIRD");
            redisQueueService.submitClassificationJob(TEST_IMAGE, "bird2.jpg", "Bird");
            redisQueueService.submitClassificationJob(TEST_IMAGE, "bird3.jpg", "BiRd");

            // Then
            Long birderSize = redisTemplate.opsForList().size(BIRDER_QUEUE);
            assertThat(birderSize).isEqualTo(3);
        }

        @Test
        @DisplayName("should route null animal type to default queue")
        void shouldRouteNullToDefaultQueue() {
            // When
            redisQueueService.submitClassificationJob(TEST_IMAGE, "test.jpg", null);

            // Then
            Long speciesnetSize = redisTemplate.opsForList().size(SPECIESNET_QUEUE);
            Long birderSize = redisTemplate.opsForList().size(BIRDER_QUEUE);

            assertThat(speciesnetSize).isEqualTo(1);
            assertThat(birderSize).isEqualTo(0);
        }

        @Test
        @DisplayName("should maintain queue independence")
        void shouldMaintainQueueIndependence() {
            // When - submit to both queues
            redisQueueService.submitClassificationJob(TEST_IMAGE, "bird1.jpg", "bird");
            redisQueueService.submitClassificationJob(TEST_IMAGE, "mammal1.jpg", "mammal");
            redisQueueService.submitClassificationJob(TEST_IMAGE, "bird2.jpg", "bird");

            // Then - queues should be independent
            Long birderSize = redisTemplate.opsForList().size(BIRDER_QUEUE);
            Long speciesnetSize = redisTemplate.opsForList().size(SPECIESNET_QUEUE);

            assertThat(birderSize).isEqualTo(2);
            assertThat(speciesnetSize).isEqualTo(1);

            // Clearing one queue shouldn't affect the other
            redisTemplate.delete(BIRDER_QUEUE);
            assertThat(redisTemplate.opsForList().size(BIRDER_QUEUE)).isEqualTo(0);
            assertThat(redisTemplate.opsForList().size(SPECIESNET_QUEUE)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("should report available when Redis is connected")
        void shouldReportAvailableWhenConnected() {
            // When
            boolean available = redisQueueService.isAvailable();

            // Then
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("should verify Redis connection")
        void shouldVerifyRedisConnection() {
            // When
            boolean available = redisQueueService.isAvailable();

            // Then
            assertThat(available).isTrue();

            // Verify we can actually perform operations
            assertThatCode(() -> {
                redisTemplate.opsForValue().set("health-check", "ok");
                String value = redisTemplate.opsForValue().get("health-check");
                assertThat(value).isEqualTo("ok");
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should track queue sizes accurately")
        void shouldTrackQueueSizesAccurately() {
            // Given - submit various jobs
            redisQueueService.submitClassificationJob(TEST_IMAGE, "bird1.jpg", "bird");
            redisQueueService.submitClassificationJob(TEST_IMAGE, "bird2.jpg", "bird");
            redisQueueService.submitClassificationJob(TEST_IMAGE, "mammal1.jpg", "mammal");

            // When
            Long birderSize = redisTemplate.opsForList().size(BIRDER_QUEUE);
            Long speciesnetSize = redisTemplate.opsForList().size(SPECIESNET_QUEUE);

            // Then
            assertThat(birderSize).isEqualTo(2);
            assertThat(speciesnetSize).isEqualTo(1);
        }

        @Test
        @DisplayName("should monitor queue statistics")
        void shouldMonitorQueueStatistics() {
            // Given - submit jobs and process some
            for (int i = 0; i < 10; i++) {
                redisQueueService.submitClassificationJob(TEST_IMAGE, "image" + i + ".jpg", "bird");
            }

            // When - process 3 jobs
            redisTemplate.opsForList().rightPop(BIRDER_QUEUE);
            redisTemplate.opsForList().rightPop(BIRDER_QUEUE);
            redisTemplate.opsForList().rightPop(BIRDER_QUEUE);

            // Then
            Long remainingJobs = redisTemplate.opsForList().size(BIRDER_QUEUE);
            assertThat(remainingJobs).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("Async Job Submission Tests")
    class AsyncJobSubmissionTests {

        @Test
        @DisplayName("should submit job asynchronously with result processor")
        void shouldSubmitJobAsyncWithProcessor() throws Exception {
            // Given
            ClassificationResultService processor = new ClassificationResultService(null, null);
            Long detectionId = 123L;

            // When
            String jobId = redisQueueService.submitClassificationJobAsync(
                    TEST_IMAGE, TEST_FILENAME, "bird", detectionId, processor);

            // Then
            assertThat(jobId).isNotBlank();

            // Verify job is in queue
            String jobJson = redisTemplate.opsForList().rightPop(BIRDER_QUEUE);
            assertThat(jobJson).isNotNull();

            Map<String, Object> job = objectMapper.readValue(jobJson, new TypeReference<>() {
            });
            assertThat(job).containsEntry("job_id", jobId);

            // Verify job is registered with processor
            assertThat(processor.getDetectionIdForJob(jobId)).isEqualTo(detectionId);
        }

        @Test
        @DisplayName("should register multiple async jobs with different detection IDs")
        void shouldRegisterMultipleAsyncJobs() {
            // Given
            ClassificationResultService processor = new ClassificationResultService(null, null);

            // When
            String jobId1 = redisQueueService.submitClassificationJobAsync(
                    TEST_IMAGE, "image1.jpg", "bird", 100L, processor);
            String jobId2 = redisQueueService.submitClassificationJobAsync(
                    TEST_IMAGE, "image2.jpg", "mammal", 200L, processor);
            String jobId3 = redisQueueService.submitClassificationJobAsync(
                    TEST_IMAGE, "image3.jpg", "bird", 300L, processor);

            // Then
            assertThat(processor.getDetectionIdForJob(jobId1)).isEqualTo(100L);
            assertThat(processor.getDetectionIdForJob(jobId2)).isEqualTo(200L);
            assertThat(processor.getDetectionIdForJob(jobId3)).isEqualTo(300L);
            assertThat(processor.getPendingJobCount()).isEqualTo(3);
        }

    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle null image bytes gracefully")
        void shouldHandleNullImageBytes() {
            // When/Then - should throw exception due to null pointer
            assertThatThrownBy(() -> redisQueueService.submitClassificationJob(null, TEST_FILENAME, "bird"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should handle empty image bytes")
        void shouldHandleEmptyImageBytes() throws Exception {
            // Given
            byte[] emptyImage = new byte[0];

            // When
            String jobId = redisQueueService.submitClassificationJob(emptyImage, TEST_FILENAME, "bird");

            // Then
            assertThat(jobId).isNotBlank();

            String jobJson = redisTemplate.opsForList().rightPop(BIRDER_QUEUE);
            Map<String, Object> job = objectMapper.readValue(jobJson, new TypeReference<>() {
            });

            String imageBase64 = (String) job.get("image_base64");
            byte[] decodedImage = Base64.getDecoder().decode(imageBase64);
            assertThat(decodedImage).isEmpty();
        }

        @Test
        @DisplayName("should handle null filename")
        void shouldHandleNullFilename() throws Exception {
            // When
            String jobId = redisQueueService.submitClassificationJob(TEST_IMAGE, null, "bird");

            // Then
            assertThat(jobId).isNotBlank();

            String jobJson = redisTemplate.opsForList().rightPop(BIRDER_QUEUE);
            Map<String, Object> job = objectMapper.readValue(jobJson, new TypeReference<>() {
            });

            assertThat(job.get("filename")).isNull();
        }

        @Test
        @DisplayName("should handle empty filename")
        void shouldHandleEmptyFilename() throws Exception {
            // When
            String jobId = redisQueueService.submitClassificationJob(TEST_IMAGE, "", "mammal");

            // Then
            assertThat(jobId).isNotBlank();

            String jobJson = redisTemplate.opsForList().rightPop(SPECIESNET_QUEUE);
            Map<String, Object> job = objectMapper.readValue(jobJson, new TypeReference<>() {
            });

            assertThat(job.get("filename")).isEqualTo("");
        }

        @Test
        @DisplayName("should handle very long filename")
        void shouldHandleVeryLongFilename() throws Exception {
            // Given
            String longFilename = "very-long-filename-" + "x".repeat(1000) + ".jpg";

            // When
            String jobId = redisQueueService.submitClassificationJob(TEST_IMAGE, longFilename, "bird");

            // Then
            assertThat(jobId).isNotBlank();

            String jobJson = redisTemplate.opsForList().rightPop(BIRDER_QUEUE);
            Map<String, Object> job = objectMapper.readValue(jobJson, new TypeReference<>() {
            });

            assertThat(job.get("filename")).isEqualTo(longFilename);
        }

        @Test
        @DisplayName("should handle Unicode characters in filename")
        void shouldHandleUnicodeInFilename() throws Exception {
            // Given
            String unicodeFilename = "鳥の写真-bird-photo-🐦.jpg";

            // When
            String jobId = redisQueueService.submitClassificationJob(TEST_IMAGE, unicodeFilename, "bird");

            // Then
            assertThat(jobId).isNotBlank();

            String jobJson = redisTemplate.opsForList().rightPop(BIRDER_QUEUE);
            Map<String, Object> job = objectMapper.readValue(jobJson, new TypeReference<>() {
            });

            assertThat(job.get("filename")).isEqualTo(unicodeFilename);
        }

        @Test
        @DisplayName("should handle concurrent job submissions without data loss")
        void shouldHandleConcurrentSubmissions() throws InterruptedException {
            // Given
            int numJobs = 50;
            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(numJobs);
            AtomicInteger successCount = new AtomicInteger(0);

            // When - submit jobs concurrently
            for (int i = 0; i < numJobs; i++) {
                final int jobNum = i;
                executor.submit(() -> {
                    try {
                        String animalType = (jobNum % 2 == 0) ? "bird" : "mammal";
                        String jobId = redisQueueService.submitClassificationJob(
                                TEST_IMAGE, "concurrent-" + jobNum + ".jpg", animalType);
                        if (jobId != null && !jobId.isEmpty()) {
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Then - all jobs should be successfully submitted
            assertThat(successCount.get()).isEqualTo(numJobs);

            Long birderSize = redisTemplate.opsForList().size(BIRDER_QUEUE);
            Long speciesnetSize = redisTemplate.opsForList().size(SPECIESNET_QUEUE);

            assertThat(birderSize + speciesnetSize).isEqualTo(numJobs);
        }
    }

    @Nested
    @DisplayName("Job Format Validation Tests")
    class JobFormatValidationTests {

        @Test
        @DisplayName("should create valid JSON job structure")
        void shouldCreateValidJsonStructure() throws Exception {
            // When
            String jobId = redisQueueService.submitClassificationJob(TEST_IMAGE, TEST_FILENAME, "bird");

            // Then
            String jobJson = redisTemplate.opsForList().rightPop(BIRDER_QUEUE);
            assertThat(jobJson).isNotNull();

            // Verify JSON is valid and well-formed
            Map<String, Object> job = objectMapper.readValue(jobJson, new TypeReference<>() {
            });
            assertThat(job).containsOnlyKeys("job_id", "image_base64", "filename", "animal_type");
        }

        @Test
        @DisplayName("should include all required job fields")
        void shouldIncludeAllRequiredFields() throws Exception {
            // When
            String jobId = redisQueueService.submitClassificationJob(TEST_IMAGE, TEST_FILENAME, "mammal");

            // Then
            String jobJson = redisTemplate.opsForList().rightPop(SPECIESNET_QUEUE);
            Map<String, Object> job = objectMapper.readValue(jobJson, new TypeReference<>() {
            });

            assertThat(job).containsKey("job_id");
            assertThat(job).containsKey("image_base64");
            assertThat(job).containsKey("filename");
            assertThat(job).containsKey("animal_type");

            assertThat(job.get("job_id")).isInstanceOf(String.class);
            assertThat(job.get("image_base64")).isInstanceOf(String.class);
            assertThat(job.get("filename")).isInstanceOf(String.class);
            assertThat(job.get("animal_type")).isInstanceOf(String.class);
        }

        @Test
        @DisplayName("should encode image data correctly in Base64")
        void shouldEncodeImageCorrectly() throws Exception {
            // Given - image with known content
            byte[] knownImage = "test-image-12345".getBytes(StandardCharsets.UTF_8);

            // When
            redisQueueService.submitClassificationJob(knownImage, TEST_FILENAME, "bird");

            // Then
            String jobJson = redisTemplate.opsForList().rightPop(BIRDER_QUEUE);
            Map<String, Object> job = objectMapper.readValue(jobJson, new TypeReference<>() {
            });

            String imageBase64 = (String) job.get("image_base64");
            assertThat(imageBase64).isNotBlank();

            // Verify decoding produces original image
            byte[] decodedImage = Base64.getDecoder().decode(imageBase64);
            assertThat(decodedImage).isEqualTo(knownImage);
            assertThat(new String(decodedImage, StandardCharsets.UTF_8)).isEqualTo("test-image-12345");
        }

        @Test
        @DisplayName("should preserve animal type in job")
        void shouldPreserveAnimalType() throws Exception {
            // When - submit with various animal types
            redisQueueService.submitClassificationJob(TEST_IMAGE, "test1.jpg", "bird");
            redisQueueService.submitClassificationJob(TEST_IMAGE, "test2.jpg", "mammal");
            redisQueueService.submitClassificationJob(TEST_IMAGE, "test3.jpg", "reptile");

            // Then - verify animal types are preserved
            String birdJob = redisTemplate.opsForList().rightPop(BIRDER_QUEUE);
            Map<String, Object> birdJobMap = objectMapper.readValue(birdJob, new TypeReference<>() {
            });
            assertThat(birdJobMap.get("animal_type")).isEqualTo("bird");

            String mammalJob = redisTemplate.opsForList().rightPop(SPECIESNET_QUEUE);
            Map<String, Object> mammalJobMap = objectMapper.readValue(mammalJob, new TypeReference<>() {
            });
            assertThat(mammalJobMap.get("animal_type")).isEqualTo("mammal");

            String reptileJob = redisTemplate.opsForList().rightPop(SPECIESNET_QUEUE);
            Map<String, Object> reptileJobMap = objectMapper.readValue(reptileJob, new TypeReference<>() {
            });
            assertThat(reptileJobMap.get("animal_type")).isEqualTo("reptile");
        }
    }
}
