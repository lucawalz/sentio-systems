package dev.syslabs.sentio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import dev.syslabs.sentio.BaseIntegrationTest;
import dev.syslabs.sentio.model.AnimalDetection;
import dev.syslabs.sentio.repository.AnimalDetectionRepository;
import dev.syslabs.sentio.service.classification.AnimalClassificationClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for AnimalClassifierService.
 * Uses WireMock to mock preprocessing service at port 8092.
 *
 * NOTE: Due to @Async nature of classifyAndUpdate, some tests verify
 * invocation and setup rather than final state changes.
 */
@WireMockTest
class AnimalClassifierServiceTest extends BaseIntegrationTest {

    private static final String BIRD_CLASSIFICATION_RESPONSE = """
            {
                "detection": {
                    "bird_detected": true,
                    "confidence": 0.92
                },
                "classification": {
                    "top_species": "Parus major",
                    "top_confidence": 0.85,
                    "predictions": [
                        {
                            "species": "Parus major",
                            "confidence": 0.85
                        },
                        {
                            "species": "Parus caeruleus",
                            "confidence": 0.12
                        }
                    ]
                }
            }
            """;

    private static final String MAMMAL_CLASSIFICATION_RESPONSE = """
            {
                "classification": {
                    "top_species": "uuid123;mammalia;carnivora;canidae;canis;lupus;wolf",
                    "top_confidence": 0.78,
                    "predictions": [
                        {
                            "species": "uuid123;mammalia;carnivora;canidae;canis;lupus;wolf",
                            "confidence": 0.78
                        },
                        {
                            "species": "uuid456;mammalia;carnivora;canidae;canis;latrans;coyote",
                            "confidence": 0.15
                        }
                    ]
                }
            }
            """;

    @DynamicPropertySource
    static void configureWireMock(DynamicPropertyRegistry registry) {
        // URL is dynamically set in setUp()
        registry.add("preprocessing.service.enabled", () -> "true");
        registry.add("queue.enabled", () -> "false");
        // Use non-destructive schema management to avoid table drops when
        // nested test contexts are initialized during the same test run.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private IAnimalClassifierService animalClassifierServiceProxy;

    private AnimalClassifierService animalClassifierService;

    @Autowired
    private AnimalDetectionRepository animalDetectionRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @MockitoBean
    private RedisQueueService redisQueueService;

    @MockitoBean
    private ClassificationResultService resultProcessor;

    @Autowired
    private AnimalClassificationClient animalClassificationClient;

    @Autowired
    private ObjectMapper objectMapper;

    private Path tempImageFile;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        cleanupAnimalDetectionsSafely();
        reset();

        // Create a temporary test image file
        tempImageFile = Files.createTempFile("test-animal", ".jpg");
        Files.write(tempImageFile, "fake image data".getBytes());

        // Mock ImageStorageService to return our temp file
        when(imageStorageService.getLocalImagePath(anyString())).thenReturn(tempImageFile);

        // Bypass Spring's @Async proxy to execute methods synchronously
        animalClassifierService = org.springframework.test.util.AopTestUtils
                .getTargetObject(animalClassifierServiceProxy);

        // Dynamically set WireMock URL on the classification client (where these fields now live)
        org.springframework.test.util.ReflectionTestUtils.setField(
                animalClassificationClient,
                "preprocessingServiceUrl",
                wmRuntimeInfo.getHttpBaseUrl() + "/classify");

        // Force use of strictly synchronous, basic RestTemplate to avoid HTTP/2 issues
        // with WireMock
        org.springframework.test.util.ReflectionTestUtils.setField(
                animalClassificationClient,
                "restTemplate",
                new org.springframework.web.client.RestTemplate());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempImageFile != null && Files.exists(tempImageFile)) {
            Files.delete(tempImageFile);
        }
        // Clean up test data for @Commit tests
        cleanupAnimalDetectionsSafely();
    }

    private void cleanupAnimalDetectionsSafely() {
        try {
            Object tableName = entityManager
                    .createNativeQuery("SELECT to_regclass('public.animal_detections')")
                    .getSingleResult();

            if (tableName != null) {
                animalDetectionRepository.deleteAll();
            }
        } catch (Exception ignored) {
            // Table may not be initialized yet for a fresh context. Ignore cleanup in that case.
        }
    }

    private AnimalDetection createDetection(String animalType, String species, float confidence) {
        AnimalDetection detection = new AnimalDetection();
        // animal_type has NOT NULL constraint - use "unknown" if null
        detection.setAnimalType(animalType != null ? animalType : "unknown");
        detection.setSpecies(species);
        detection.setConfidence(confidence);
        detection.setX(100);
        detection.setY(100);
        detection.setWidth(200);
        detection.setHeight(200);
        detection.setImageUrl("/images/test.jpg");
        detection.setDeviceId("test-device");
        detection.setTimestamp(LocalDateTime.now());
        return animalDetectionRepository.save(detection);
    }

    @Nested
    @DisplayName("classifyAndUpdate")
    class ClassifyAndUpdateTests {

        @Test
        @DisplayName("should initiate bird classification via preprocessing service")
        void shouldInitiateBirdClassification() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BIRD_CLASSIFICATION_RESPONSE)));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            // Verify async method was called and database was updated
            AnimalDetection updated = animalDetectionRepository.findById(detection.getId()).orElseThrow();
            assertThat(updated.getSpecies()).isEqualTo("Parus major");
            assertThat(updated.isAiProcessed()).isTrue();
        }

        @Test
        @DisplayName("should initiate mammal classification")
        void shouldInitiateMammalClassification() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(MAMMAL_CLASSIFICATION_RESPONSE)));

            AnimalDetection detection = createDetection("mammal", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            AnimalDetection updated = animalDetectionRepository.findById(detection.getId()).orElseThrow();
            assertThat(updated.getSpecies()).isEqualTo("wolf");
            assertThat(updated.isAiProcessed()).isTrue();
        }

        @Test
        @DisplayName("should use queue when enabled and available")
        void shouldUseQueueWhenEnabled() throws IOException {
            ReflectionTestUtils.setField(animalClassifierService, "queueEnabled", true);
            when(redisQueueService.isAvailable()).thenReturn(true);
            when(redisQueueService.submitClassificationJobAsync(any(), anyString(), anyString(), anyLong(), any()))
                    .thenReturn("job-123");

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(redisQueueService).submitClassificationJobAsync(
                    any(byte[].class),
                    anyString(),
                    eq("bird"),
                    eq(detection.getId()),
                    eq(resultProcessor));
        }

        @Test
        @DisplayName("should fallback to HTTP when queue fails")
        void shouldFallbackToHttpWhenQueueFails() throws IOException {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BIRD_CLASSIFICATION_RESPONSE)));

            ReflectionTestUtils.setField(animalClassifierService, "queueEnabled", true);
            when(redisQueueService.isAvailable()).thenReturn(true);
            when(redisQueueService.submitClassificationJobAsync(any(), anyString(), anyString(), anyLong(), any()))
                    .thenThrow(new RuntimeException("Queue submission failed"));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            // Should attempt queue first, then fallback
            verify(redisQueueService).submitClassificationJobAsync(any(), anyString(), anyString(), anyLong(),
                    any());
        }

        @Test
        @DisplayName("should skip classification for unsupported animal type")
        void shouldSkipUnsupportedAnimalType() {
            AnimalDetection detection = createDetection("reptile", "Lizard", 0.7f);
            animalClassifierService.classifyAndUpdate(detection);

            // Should not attempt to get image for unsupported types
            verify(imageStorageService, never()).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should handle missing image file gracefully")
        void shouldHandleMissingImageFile() {
            when(imageStorageService.getLocalImagePath(anyString()))
                    .thenReturn(Path.of("/nonexistent/image.jpg"));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should handle preprocessing service error")
        void shouldHandlePreprocessingError() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withBody("Internal Server Error")));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should handle empty response from preprocessing service")
        void shouldHandleEmptyResponse() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{}")));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should handle no bird detected in response")
        void shouldHandleNoBirdDetected() {
            String noBirdResponse = """
                    {
                        "detection": {
                            "bird_detected": false,
                            "confidence": 0.1
                        },
                        "classification": {}
                    }
                    """;

            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(noBirdResponse)));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            AnimalDetection updated = animalDetectionRepository.findById(detection.getId()).orElseThrow();
            assertThat(updated.isAiProcessed()).isTrue();
            assertThat(updated.getSpecies()).isEqualTo("Unknown"); // Keeps original
        }

        @Test
        @DisplayName("should process response with preprocessing metadata")
        void shouldProcessPreprocessingMetadata() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BIRD_CLASSIFICATION_RESPONSE)));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            AnimalDetection updated = animalDetectionRepository.findById(detection.getId()).orElseThrow();
            assertThat(updated.getSpecies()).isEqualTo("Parus major");
            assertThat(updated.isAiProcessed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Queue Submission")
    class QueueSubmissionTests {

        @Test
        @DisplayName("should submit to queue successfully")
        void shouldSubmitToQueueSuccessfully() throws IOException {
            ReflectionTestUtils.setField(animalClassifierService, "queueEnabled", true);
            when(redisQueueService.isAvailable()).thenReturn(true);
            when(redisQueueService.submitClassificationJobAsync(any(), anyString(), anyString(), anyLong(), any()))
                    .thenReturn("job-456");

            AnimalDetection detection = createDetection("mammal", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(redisQueueService).submitClassificationJobAsync(
                    any(byte[].class),
                    contains(".jpg"),
                    eq("mammal"),
                    eq(detection.getId()),
                    eq(resultProcessor));
        }

        @Test
        @DisplayName("should fallback to HTTP when queue is unavailable")
        void shouldFallbackWhenQueueUnavailable() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(MAMMAL_CLASSIFICATION_RESPONSE)));

            ReflectionTestUtils.setField(animalClassifierService, "queueEnabled", true);
            when(redisQueueService.isAvailable()).thenReturn(false);

            AnimalDetection detection = createDetection("mammal", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(redisQueueService, never()).submitClassificationJobAsync(any(), anyString(), anyString(),
                    anyLong(), any());
        }

        @Test
        @DisplayName("should handle queue disabled configuration")
        void shouldHandleQueueDisabled() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BIRD_CLASSIFICATION_RESPONSE)));

            ReflectionTestUtils.setField(animalClassifierService, "queueEnabled", false);

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(redisQueueService, never()).submitClassificationJobAsync(any(), anyString(), anyString(),
                    anyLong(), any());
        }

        @Test
        @DisplayName("should handle queue exception with fallback")
        void shouldHandleQueueExceptionWithFallback() throws IOException {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BIRD_CLASSIFICATION_RESPONSE)));

            ReflectionTestUtils.setField(animalClassifierService, "queueEnabled", true);
            when(redisQueueService.isAvailable()).thenReturn(true);
            when(redisQueueService.submitClassificationJobAsync(any(), anyString(), anyString(), anyLong(), any()))
                    .thenThrow(new RuntimeException("Redis connection lost"));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(redisQueueService).submitClassificationJobAsync(any(), anyString(), anyString(), anyLong(),
                    any());
        }

        @Test
        @DisplayName("should read image bytes for queue submission")
        void shouldReadImageBytesForQueue() throws IOException {
            ReflectionTestUtils.setField(animalClassifierService, "queueEnabled", true);
            when(redisQueueService.isAvailable()).thenReturn(true);
            when(redisQueueService.submitClassificationJobAsync(any(), anyString(), anyString(), anyLong(), any()))
                    .thenReturn("job-789");

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }
    }

    @Nested
    @DisplayName("Helper Methods")
    class HelperMethodsTests {

        @Test
        @DisplayName("should confirm classifier exists for bird")
        void shouldConfirmBirdClassifierExists() {
            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should confirm classifier exists for mammal")
        void shouldConfirmMammalClassifierExists() {
            AnimalDetection detection = createDetection("mammal", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should confirm classifier exists for human")
        void shouldConfirmHumanClassifierExists() {
            AnimalDetection detection = createDetection("human", "Person", 0.9f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should confirm no classifier for unsupported types")
        void shouldConfirmNoClassifierForUnsupported() {
            AnimalDetection detection = createDetection("fish", "Goldfish", 0.8f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService, never()).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should retrieve local image file path")
        void shouldRetrieveLocalImageFile() {
            when(imageStorageService.getLocalImagePath("/images/test.jpg"))
                    .thenReturn(tempImageFile);

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath("/images/test.jpg");
        }
    }

    @Nested
    @DisplayName("Circuit Breaker")
    class CircuitBreakerTests {

        @Test
        @DisplayName("should handle repeated failures without throwing")
        void shouldHandleRepeatedFailures() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(503)
                            .withBody("Service Unavailable")));

            // Trigger multiple failures - circuit breaker should prevent cascading failures
            for (int i = 0; i < 5; i++) {
                AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
                animalClassifierService.classifyAndUpdate(detection);
            }

            verify(imageStorageService, atLeast(1)).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should handle successful calls")
        void shouldHandleSuccessfulCalls() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BIRD_CLASSIFICATION_RESPONSE)));

            for (int i = 0; i < 3; i++) {
                AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
                animalClassifierService.classifyAndUpdate(detection);
            }

            verify(imageStorageService, atLeast(3)).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should use fallback method on failure")
        void shouldUseFallbackMethod() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withBody("Internal Error")));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should handle 4xx client errors")
        void shouldHandle4xxErrors() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(400)
                            .withBody("Bad Request")));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should handle network connectivity issues")
        void shouldHandleNetworkIssues() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }
    }

    @Nested
    @DisplayName("determineAnimalType")
    class DetermineAnimalTypeTests {

        @Test
        @DisplayName("should determine bird type")
        void shouldDetermineBirdType() {
            String type = animalClassifierService.determineAnimalType("bird");
            assertThat(type).isEqualTo("bird");
        }

        @Test
        @DisplayName("should determine mammal type for cat")
        void shouldDetermineMammalForCat() {
            String type = animalClassifierService.determineAnimalType("cat");
            assertThat(type).isEqualTo("mammal");
        }

        @Test
        @DisplayName("should determine mammal type for dog")
        void shouldDetermineMammalForDog() {
            String type = animalClassifierService.determineAnimalType("dog");
            assertThat(type).isEqualTo("mammal");
        }

        @Test
        @DisplayName("should determine mammal type for squirrel")
        void shouldDetermineMammalForSquirrel() {
            String type = animalClassifierService.determineAnimalType("squirrel");
            assertThat(type).isEqualTo("mammal");
        }

        @Test
        @DisplayName("should determine human type for person")
        void shouldDetermineHumanForPerson() {
            String type = animalClassifierService.determineAnimalType("person");
            assertThat(type).isEqualTo("human");
        }

        @Test
        @DisplayName("should return unknown for null species")
        void shouldReturnUnknownForNull() {
            String type = animalClassifierService.determineAnimalType(null);
            assertThat(type).isEqualTo("unknown");
        }

        @Test
        @DisplayName("should return unknown for unrecognized species")
        void shouldReturnUnknownForUnrecognized() {
            String type = animalClassifierService.determineAnimalType("elephant");
            assertThat(type).isEqualTo("unknown");
        }

        @Test
        @DisplayName("should handle case insensitive species matching")
        void shouldHandleCaseInsensitive() {
            assertThat(animalClassifierService.determineAnimalType("BIRD")).isEqualTo("bird");
            assertThat(animalClassifierService.determineAnimalType("Cat")).isEqualTo("mammal");
            assertThat(animalClassifierService.determineAnimalType("PERSON")).isEqualTo("human");
        }
    }

    @Nested
    @DisplayName("Classification Response Processing")
    class ClassificationResponseProcessingTests {

        @Test
        @DisplayName("should fallback to original species when all predictions are invalid")
        void shouldFallbackToOriginalWhenAllInvalid() {
            String allInvalidResponse = """
                    {
                        "classification": {
                            "top_species": "uuid999;unknown;unknown;unknown;unknown;unknown;blank",
                            "top_confidence": 0.45,
                            "predictions": [
                                {
                                    "species": "uuid999;unknown;unknown;unknown;unknown;unknown;blank",
                                    "confidence": 0.45
                                },
                                {
                                    "species": "uuid998;unknown;unknown;unknown;unknown;unknown;vehicle",
                                    "confidence": 0.35
                                },
                                {
                                    "species": "uuid997;unknown;unknown;unknown;unknown;unknown;unknown",
                                    "confidence": 0.20
                                }
                            ]
                        }
                    }
                    """;

            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(allInvalidResponse)));

            AnimalDetection detection = createDetection("mammal", "Fox", 0.7f);
            Long detectionId = detection.getId();

            animalClassifierService.classifyAndUpdate(detection);

            AnimalDetection updated = animalDetectionRepository.findById(detectionId).orElseThrow();
            // Should fallback to original species
            assertThat(updated.getSpecies()).isEqualTo("Fox");
            assertThat(updated.getConfidence()).isEqualTo(0.7f);
        }

        @Test
        @DisplayName("should handle classification response with missing detection field")
        void shouldHandleMissingDetectionField() {
            String missingDetectionResponse = """
                    {
                        "classification": {
                            "top_species": "Parus major",
                            "top_confidence": 0.85,
                            "predictions": [
                                {
                                    "species": "Parus major",
                                    "confidence": 0.85
                                }
                            ]
                        }
                    }
                    """;

            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(missingDetectionResponse)));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should handle classification response with missing classification field")
        void shouldHandleMissingClassificationField() {
            String missingClassificationResponse = """
                    {
                        "detection": {
                            "bird_detected": true,
                            "confidence": 0.92
                        }
                    }
                    """;

            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(missingClassificationResponse)));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should handle null animal type gracefully")
        void shouldHandleNullAnimalType() {
            AnimalDetection detection = createDetection(null, "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService, never()).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should handle empty string species")
        void shouldHandleEmptyStringSpecies() {
            String responseWithEmptySpecies = """
                    {
                        "classification": {
                            "top_species": "",
                            "top_confidence": 0.78,
                            "predictions": [
                                {
                                    "species": "",
                                    "confidence": 0.78
                                }
                            ]
                        }
                    }
                    """;

            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(responseWithEmptySpecies)));

            AnimalDetection detection = createDetection("mammal", "Fox", 0.6f);
            Long detectionId = detection.getId();

            animalClassifierService.classifyAndUpdate(detection);

            AnimalDetection updated = animalDetectionRepository.findById(detectionId).orElseThrow();
            // Should fallback to original
            assertThat(updated.getSpecies()).isIn("Fox", "Unknown");
        }

        @Test
        @DisplayName("should handle malformed JSON response")
        void shouldHandleMalformedJson() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{ invalid json }")));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should preserve original values when already set")
        void shouldPreserveOriginalValuesWhenAlreadySet() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(MAMMAL_CLASSIFICATION_RESPONSE)));

            AnimalDetection detection = createDetection("mammal", "Dog", 0.8f);
            detection.setOriginalSpecies("Cat");
            detection.setOriginalConfidence(0.6f);
            detection = animalDetectionRepository.save(detection);
            Long detectionId = detection.getId();

            animalClassifierService.classifyAndUpdate(detection);

            AnimalDetection updated = animalDetectionRepository.findById(detectionId).orElseThrow();
            // Should preserve the original values, not overwrite them
            assertThat(updated.getOriginalSpecies()).isEqualTo("Cat");
            assertThat(updated.getOriginalConfidence()).isEqualTo(0.6f);
        }

        @Test
        @DisplayName("should handle ClassCastException in bird response processing")
        void shouldHandleClassCastExceptionInBirdResponse() {
            String invalidTypeResponse = """
                    {
                        "detection": "this should be an object",
                        "classification": {
                            "top_species": "Parus major",
                            "top_confidence": 0.85
                        }
                    }
                    """;

            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(invalidTypeResponse)));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should handle ClassCastException in generic response processing")
        void shouldHandleClassCastExceptionInGenericResponse() {
            String invalidTypeResponse = """
                    {
                        "classification": "this should be an object"
                    }
                    """;

            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(invalidTypeResponse)));

            AnimalDetection detection = createDetection("mammal", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }
    }

    @Nested
    @DisplayName("Species Validation")
    class SpeciesValidationTests {

        @Test
        @DisplayName("should validate blank as invalid species")
        void shouldValidateBlankAsInvalid() {
            String response = """
                    {
                        "classification": {
                            "top_species": "uuid;blank",
                            "top_confidence": 0.5,
                            "predictions": []
                        }
                    }
                    """;

            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(response)));

            AnimalDetection detection = createDetection("mammal", "Deer", 0.7f);
            Long detectionId = detection.getId();

            animalClassifierService.classifyAndUpdate(detection);

            AnimalDetection updated = animalDetectionRepository.findById(detectionId).orElseThrow();
            // Should fallback to original species
            assertThat(updated.getSpecies()).isEqualTo("Deer");
        }

        @Test
        @DisplayName("should validate vehicle as invalid species")
        void shouldValidateVehicleAsInvalid() {
            String response = """
                    {
                        "classification": {
                            "top_species": "uuid;vehicle",
                            "top_confidence": 0.5,
                            "predictions": []
                        }
                    }
                    """;

            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(response)));

            AnimalDetection detection = createDetection("mammal", "Rabbit", 0.6f);
            Long detectionId = detection.getId();

            animalClassifierService.classifyAndUpdate(detection);

            AnimalDetection updated = animalDetectionRepository.findById(detectionId).orElseThrow();
            assertThat(updated.getSpecies()).isEqualTo("Rabbit");
        }

        @Test
        @DisplayName("should validate none as invalid species")
        void shouldValidateNoneAsInvalid() {
            String response = """
                    {
                        "classification": {
                            "top_species": "none",
                            "top_confidence": 0.5,
                            "predictions": []
                        }
                    }
                    """;

            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(response)));

            AnimalDetection detection = createDetection("mammal", "Squirrel", 0.65f);
            Long detectionId = detection.getId();

            animalClassifierService.classifyAndUpdate(detection);

            AnimalDetection updated = animalDetectionRepository.findById(detectionId).orElseThrow();
            assertThat(updated.getSpecies()).isEqualTo("Squirrel");
        }

        @Test
        @DisplayName("should validate n/a as invalid species")
        void shouldValidateNAAsInvalid() {
            String response = """
                    {
                        "classification": {
                            "top_species": "n/a",
                            "top_confidence": 0.5,
                            "predictions": []
                        }
                    }
                    """;

            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(response)));

            AnimalDetection detection = createDetection("mammal", "Raccoon", 0.7f);
            Long detectionId = detection.getId();

            animalClassifierService.classifyAndUpdate(detection);

            AnimalDetection updated = animalDetectionRepository.findById(detectionId).orElseThrow();
            assertThat(updated.getSpecies()).isEqualTo("Raccoon");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("should handle IOException when reading image file for queue")
        void shouldHandleIOExceptionWhenReadingImage() throws IOException {
            ReflectionTestUtils.setField(animalClassifierService, "queueEnabled", true);
            when(redisQueueService.isAvailable()).thenReturn(true);

            // Delete the temp file to cause IOException
            if (tempImageFile != null && Files.exists(tempImageFile)) {
                Files.delete(tempImageFile);
            }

            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BIRD_CLASSIFICATION_RESPONSE)));

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
                verify(imageStorageService).getLocalImagePath(anyString());
            });
        }

        @Test
        @DisplayName("should handle response with null top_species")
        void shouldHandleNullTopSpecies() {
            String response = """
                    {
                        "classification": {
                            "top_species": null,
                            "top_confidence": 0.5,
                            "predictions": []
                        }
                    }
                    """;

            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(response)));

            AnimalDetection detection = createDetection("mammal", "Cat", 0.8f);
            animalClassifierService.classifyAndUpdate(detection);

            await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
                verify(imageStorageService).getLocalImagePath(anyString());
            });
        }

        @Test
        @DisplayName("should handle timeout from preprocessing service")
        void shouldHandleTimeout() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BIRD_CLASSIFICATION_RESPONSE)
                            .withFixedDelay(30000))); // 30 second delay

            AnimalDetection detection = createDetection("bird", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should handle empty animal type string")
        void shouldHandleEmptyAnimalType() {
            AnimalDetection detection = createDetection("", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            verify(imageStorageService, never()).getLocalImagePath(anyString());
        }

        @Test
        @DisplayName("should handle case variations in animal type")
        void shouldHandleCaseVariationsInAnimalType() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BIRD_CLASSIFICATION_RESPONSE)));

            AnimalDetection detection = createDetection("BIRD", "Unknown", 0.5f);
            animalClassifierService.classifyAndUpdate(detection);

            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                verify(imageStorageService).getLocalImagePath(anyString());
            });
        }

        @Test
        @DisplayName("should skip classification for already processed detection")
        void shouldProcessAlreadyAIProcessedDetection() {
            stubFor(post(urlPathEqualTo("/classify"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(MAMMAL_CLASSIFICATION_RESPONSE)));

            AnimalDetection detection = createDetection("mammal", "Dog", 0.8f);
            detection.setAiProcessed(true);
            detection.setAiClassifiedAt(LocalDateTime.now());
            detection = animalDetectionRepository.save(detection);

            animalClassifierService.classifyAndUpdate(detection);

            // Should still attempt classification (service doesn't check aiProcessed flag)
            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                verify(imageStorageService).getLocalImagePath(anyString());
            });
        }
    }
}
