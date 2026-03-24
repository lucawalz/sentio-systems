package dev.syslabs.sentio.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.syslabs.sentio.BaseIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for N8nWorkflowTriggerService.
 * Uses WireMock to mock n8n webhook endpoints.
 *
 * Test Strategy:
 * - Test all public methods with real HTTP client execution
 * - Use WireMock for external n8n webhook API calls
 * - Test success and error scenarios
 * - Test payload construction and HTTP request handling
 * - Test various workflow types (weather summary, sightings summary, AI agent,
 * animal detection)
 * - Test error handling and exception scenarios
 * - Test edge cases (null values, empty strings, network failures)
 *
 * Coverage Goals:
 * - triggerWeatherSummary() - user workflow triggering
 * - triggerSightingsSummary() - user workflow triggering
 * - triggerAiAgent() - AI agent with query and response parsing
 * - triggerAnimalDetectionWorkflow() - device workflow with multiple parameters
 * - triggerWebhook() - private method exercised through public methods
 * - Error handling for network failures, 4xx/5xx responses
 * - Payload construction with various data types
 * - URL construction with base URL and workflow suffix
 *
 * Target Coverage: 80%+ (from 1.5%)
 */
@SpringBootTest
class N8nWorkflowTriggerServiceIT extends BaseIntegrationTest {

        private static final WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

        static {
                wireMockServer.start();
                configureFor("localhost", wireMockServer.port());
        }

        @Autowired
        private N8nWorkflowTriggerService n8nWorkflowTriggerService;

        @BeforeEach
        void resetWireMock() {
                wireMockServer.resetAll();
        }

        @AfterAll
        static void stopWireMock() {
                wireMockServer.stop();
        }

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                // Point n8n webhook base URL to WireMock server
                registry.add("n8n.webhook.base-url", () -> wireMockServer.baseUrl() + "/webhook");
                registry.add("n8n.workflow.suffix", () -> "");
        }

        @TestConfiguration
        static class TestConfig {
                @Bean
                @Primary
                public RestTemplate restTemplate(RestTemplateBuilder builder) {
                        return builder
                                        .requestFactory(SimpleClientHttpRequestFactory::new)
                                        .build();
                }
        }

        @Nested
        @DisplayName("Weather Summary Workflow Tests")
        class WeatherSummaryTests {

                @Test
                @DisplayName("Should successfully trigger weather summary workflow for valid user")
                void triggerWeatherSummary_withValidUserId_shouldReturnTrue() {
                        // Arrange
                        String userId = "test-user-123";
                        stubFor(post(urlEqualTo("/webhook/sentio-weather-summary"))
                                        .withRequestBody(matchingJsonPath("$.userId", equalTo(userId)))
                                        .willReturn(aResponse()
                                                        .withStatus(200)
                                                        .withHeader("Content-Type", "application/json")
                                                        .withBody("{\"status\":\"success\"}")));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerWeatherSummary(userId);

                        // Assert
                        assertThat(result).isTrue();
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-weather-summary"))
                                        .withRequestBody(matchingJsonPath("$.userId")));
                }

                @Test
                @DisplayName("Should handle 4xx client error response")
                void triggerWeatherSummary_with4xxError_shouldReturnFalse() {
                        // Arrange
                        String userId = "invalid-user";
                        stubFor(post(urlEqualTo("/webhook/sentio-weather-summary"))
                                        .willReturn(aResponse()
                                                        .withStatus(400)
                                                        .withBody("{\"error\":\"Bad Request\"}")));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerWeatherSummary(userId);

                        // Assert
                        assertThat(result).isFalse();
                }

                @Test
                @DisplayName("Should handle 5xx server error response")
                void triggerWeatherSummary_with5xxError_shouldReturnFalse() {
                        // Arrange
                        String userId = "test-user-123";
                        stubFor(post(urlEqualTo("/webhook/sentio-weather-summary"))
                                        .willReturn(aResponse()
                                                        .withStatus(500)
                                                        .withBody("{\"error\":\"Internal Server Error\"}")));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerWeatherSummary(userId);

                        // Assert
                        assertThat(result).isFalse();
                }

                @Test
                @DisplayName("Should handle network timeout/connection error")
                void triggerWeatherSummary_withNetworkError_shouldReturnFalse() {
                        // Arrange
                        String userId = "test-user-123";
                        stubFor(post(urlEqualTo("/webhook/sentio-weather-summary"))
                                        .willReturn(aResponse()
                                                        .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

                        // Act - use a shorter timeout for testing
                        boolean result = n8nWorkflowTriggerService.triggerWeatherSummary(userId);

                        // Assert
                        assertThat(result).isFalse();
                }

                @ParameterizedTest
                @ValueSource(strings = { "user-1", "keycloak-uuid-123-456", "admin@example.com" })
                @DisplayName("Should handle various user ID formats")
                void triggerWeatherSummary_withVariousUserIds_shouldSucceed(String userId) {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-weather-summary"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerWeatherSummary(userId);

                        // Assert
                        assertThat(result).isTrue();
                }
        }

        @Nested
        @DisplayName("Sightings Summary Workflow Tests")
        class SightingsSummaryTests {

                @Test
                @DisplayName("Should successfully trigger sightings summary workflow")
                void triggerSightingsSummary_withValidUserId_shouldReturnTrue() {
                        // Arrange
                        String userId = "test-user-456";
                        stubFor(post(urlEqualTo("/webhook/sentio-sightings-summary"))
                                        .withRequestBody(matchingJsonPath("$.userId", equalTo(userId)))
                                        .willReturn(aResponse()
                                                        .withStatus(200)
                                                        .withHeader("Content-Type", "application/json")
                                                        .withBody("{\"status\":\"success\"}")));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerSightingsSummary(userId);

                        // Assert
                        assertThat(result).isTrue();
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-sightings-summary"))
                                        .withHeader("Content-Type", equalTo("application/json")));
                }

                @Test
                @DisplayName("Should handle 404 Not Found error")
                void triggerSightingsSummary_with404Error_shouldReturnFalse() {
                        // Arrange
                        String userId = "test-user-456";
                        stubFor(post(urlEqualTo("/webhook/sentio-sightings-summary"))
                                        .willReturn(aResponse().withStatus(404)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerSightingsSummary(userId);

                        // Assert
                        assertThat(result).isFalse();
                }

                @Test
                @DisplayName("Should send correct Content-Type header")
                void triggerSightingsSummary_shouldSendJsonContentType() {
                        // Arrange
                        String userId = "test-user-789";
                        stubFor(post(urlEqualTo("/webhook/sentio-sightings-summary"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        n8nWorkflowTriggerService.triggerSightingsSummary(userId);

                        // Assert
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-sightings-summary"))
                                        .withHeader("Content-Type", containing("application/json")));
                }
        }

        @Nested
        @DisplayName("AI Agent Workflow Tests")
        class AiAgentTests {

                @Test
                @DisplayName("Should successfully trigger AI agent and parse response")
                void triggerAiAgent_withValidQuery_shouldReturnResponse() {
                        // Arrange
                        String userId = "ai-user-123";
                        String query = "What is the weather like today?";
                        String expectedResponse = "The weather is sunny with a temperature of 22°C.";

                        stubFor(post(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .withRequestBody(matchingJsonPath("$.userId", equalTo(userId)))
                                        .withRequestBody(matchingJsonPath("$.query", equalTo(query)))
                                        .willReturn(aResponse()
                                                        .withStatus(200)
                                                        .withHeader("Content-Type", "application/json")
                                                        .withBody("{\"response\":\"" + expectedResponse + "\"}")));

                        // Act
                        String result = n8nWorkflowTriggerService.triggerAiAgent(userId, query);

                        // Assert
                        assertThat(result).isEqualTo(expectedResponse);
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .withRequestBody(matchingJsonPath("$.userId"))
                                        .withRequestBody(matchingJsonPath("$.query")));
                }

                @Test
                @DisplayName("Should handle response without 'response' field")
                void triggerAiAgent_withoutResponseField_shouldReturnDefaultMessage() {
                        // Arrange
                        String userId = "ai-user-456";
                        String query = "Test query";
                        stubFor(post(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .willReturn(aResponse()
                                                        .withStatus(200)
                                                        .withHeader("Content-Type", "application/json")
                                                        .withBody("{\"status\":\"completed\"}")));

                        // Act
                        String result = n8nWorkflowTriggerService.triggerAiAgent(userId, query);

                        // Assert
                        assertThat(result).isEqualTo("Workflow executed");
                }

                @Test
                @DisplayName("Should handle null response body")
                void triggerAiAgent_withNullResponseBody_shouldReturnNull() {
                        // Arrange
                        String userId = "ai-user-789";
                        String query = "Test query";
                        stubFor(post(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .willReturn(aResponse()
                                                        .withStatus(204))); // No content

                        // Act
                        String result = n8nWorkflowTriggerService.triggerAiAgent(userId, query);

                        // Assert
                        assertThat(result).isNull();
                }

                @Test
                @DisplayName("Should handle 4xx error response")
                void triggerAiAgent_with4xxError_shouldReturnNull() {
                        // Arrange
                        String userId = "ai-user-error";
                        String query = "Invalid query";
                        stubFor(post(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .willReturn(aResponse()
                                                        .withStatus(400)
                                                        .withBody("{\"error\":\"Bad Request\"}")));

                        // Act
                        String result = n8nWorkflowTriggerService.triggerAiAgent(userId, query);

                        // Assert
                        assertThat(result).isNull();
                }

                @Test
                @DisplayName("Should handle 5xx error response")
                void triggerAiAgent_with5xxError_shouldReturnNull() {
                        // Arrange
                        String userId = "ai-user-123";
                        String query = "Test query";
                        stubFor(post(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .willReturn(aResponse()
                                                        .withStatus(503)
                                                        .withBody("{\"error\":\"Service Unavailable\"}")));

                        // Act
                        String result = n8nWorkflowTriggerService.triggerAiAgent(userId, query);

                        // Assert
                        assertThat(result).isNull();
                }

                @Test
                @DisplayName("Should handle network connection error")
                void triggerAiAgent_withConnectionError_shouldReturnNull() {
                        // Arrange
                        String userId = "ai-user-123";
                        String query = "Test query";
                        stubFor(post(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .willReturn(aResponse()
                                                        .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

                        // Act
                        String result = n8nWorkflowTriggerService.triggerAiAgent(userId, query);

                        // Assert
                        assertThat(result).isNull();
                }

                @ParameterizedTest
                @CsvSource({
                                "user-1, What is the temperature?",
                                "user-2, Show me recent animal sightings",
                                "user-3, What alerts are active?",
                                "user-4, Generate a weather report"
                })
                @DisplayName("Should handle various user queries")
                void triggerAiAgent_withVariousQueries_shouldSucceed(String userId, String query) {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .willReturn(aResponse()
                                                        .withStatus(200)
                                                        .withHeader("Content-Type", "application/json")
                                                        .withBody("{\"response\":\"AI response\"}")));

                        // Act
                        String result = n8nWorkflowTriggerService.triggerAiAgent(userId, query);

                        // Assert
                        assertThat(result).isNotNull();
                }

                @Test
                @DisplayName("Should handle complex JSON response")
                void triggerAiAgent_withComplexResponse_shouldExtractResponseField() {
                        // Arrange
                        String userId = "ai-user-complex";
                        String query = "Complex query";
                        stubFor(post(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .willReturn(aResponse()
                                                        .withStatus(200)
                                                        .withHeader("Content-Type", "application/json")
                                                        .withBody("{\"response\":\"AI answer\",\"metadata\":{\"model\":\"groq\",\"tokens\":150}}")));

                        // Act
                        String result = n8nWorkflowTriggerService.triggerAiAgent(userId, query);

                        // Assert
                        assertThat(result).isEqualTo("AI answer");
                }

                @Test
                @DisplayName("Should send correct payload structure")
                void triggerAiAgent_shouldSendCorrectPayload() {
                        // Arrange
                        String userId = "payload-test-user";
                        String query = "Test payload";
                        stubFor(post(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .willReturn(aResponse().withStatus(200).withBody("{}")));

                        // Act
                        n8nWorkflowTriggerService.triggerAiAgent(userId, query);

                        // Assert
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .withHeader("Content-Type", containing("application/json"))
                                        .withRequestBody(matchingJsonPath("$.userId", equalTo(userId)))
                                        .withRequestBody(matchingJsonPath("$.query", equalTo(query))));
                }
        }

        @Nested
        @DisplayName("Animal Detection Workflow Tests")
        class AnimalDetectionTests {

                @Test
                @DisplayName("Should successfully trigger animal detection workflow")
                void triggerAnimalDetectionWorkflow_withValidData_shouldReturnTrue() {
                        // Arrange
                        String deviceId = "device-001";
                        String species = "Red Fox";
                        float confidence = 0.95f;

                        stubFor(post(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .withRequestBody(matchingJsonPath("$.deviceId", equalTo(deviceId)))
                                        .withRequestBody(matchingJsonPath("$.species", equalTo(species)))
                                        .withRequestBody(matchingJsonPath("$.confidence", equalTo("0.95")))
                                        .withRequestBody(matchingJsonPath("$.timestamp"))
                                        .willReturn(aResponse()
                                                        .withStatus(200)
                                                        .withHeader("Content-Type", "application/json")
                                                        .withBody("{\"status\":\"notified\"}")));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow(deviceId, species,
                                        confidence);

                        // Assert
                        assertThat(result).isTrue();
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .withRequestBody(matchingJsonPath("$.deviceId"))
                                        .withRequestBody(matchingJsonPath("$.species"))
                                        .withRequestBody(matchingJsonPath("$.confidence"))
                                        .withRequestBody(matchingJsonPath("$.timestamp")));
                }

                @ParameterizedTest
                @CsvSource({
                                "device-001, Red Fox, 0.95",
                                "device-002, European Badger, 0.87",
                                "device-003, Roe Deer, 0.92",
                                "device-004, Wild Boar, 0.99"
                })
                @DisplayName("Should handle various species detections")
                void triggerAnimalDetectionWorkflow_withVariousSpecies_shouldSucceed(
                                String deviceId, String species, float confidence) {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow(deviceId, species,
                                        confidence);

                        // Assert
                        assertThat(result).isTrue();
                }

                @Test
                @DisplayName("Should handle low confidence detection")
                void triggerAnimalDetectionWorkflow_withLowConfidence_shouldStillTrigger() {
                        // Arrange
                        String deviceId = "device-low-conf";
                        String species = "Unknown Animal";
                        float confidence = 0.55f;

                        stubFor(post(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow(deviceId, species,
                                        confidence);

                        // Assert
                        assertThat(result).isTrue();
                }

                @Test
                @DisplayName("Should handle high confidence detection")
                void triggerAnimalDetectionWorkflow_withHighConfidence_shouldSucceed() {
                        // Arrange
                        String deviceId = "device-high-conf";
                        String species = "Red Fox";
                        float confidence = 0.99f;

                        stubFor(post(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow(deviceId, species,
                                        confidence);

                        // Assert
                        assertThat(result).isTrue();
                }

                @Test
                @DisplayName("Should handle 4xx error response")
                void triggerAnimalDetectionWorkflow_with4xxError_shouldReturnFalse() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .willReturn(aResponse().withStatus(400)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow(
                                        "device-001", "Red Fox", 0.95f);

                        // Assert
                        assertThat(result).isFalse();
                }

                @Test
                @DisplayName("Should handle 5xx error response")
                void triggerAnimalDetectionWorkflow_with5xxError_shouldReturnFalse() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .willReturn(aResponse().withStatus(500)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow(
                                        "device-001", "Red Fox", 0.95f);

                        // Assert
                        assertThat(result).isFalse();
                }

                @Test
                @DisplayName("Should handle network error")
                void triggerAnimalDetectionWorkflow_withNetworkError_shouldReturnFalse() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .willReturn(aResponse()
                                                        .withFault(com.github.tomakehurst.wiremock.http.Fault.EMPTY_RESPONSE)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow(
                                        "device-001", "Red Fox", 0.95f);

                        // Assert
                        assertThat(result).isFalse();
                }

                @Test
                @DisplayName("Should include timestamp in payload")
                void triggerAnimalDetectionWorkflow_shouldIncludeTimestamp() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow("device-001", "Red Fox", 0.95f);

                        // Assert
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .withRequestBody(matchingJsonPath("$.timestamp")));
                }

                @Test
                @DisplayName("Should send correct payload structure")
                void triggerAnimalDetectionWorkflow_shouldSendCorrectPayload() {
                        // Arrange
                        String deviceId = "payload-test-device";
                        String species = "Test Species";
                        float confidence = 0.88f;

                        stubFor(post(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow(deviceId, species, confidence);

                        // Assert
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .withHeader("Content-Type", containing("application/json"))
                                        .withRequestBody(matchingJsonPath("$.deviceId", equalTo(deviceId)))
                                        .withRequestBody(matchingJsonPath("$.species", equalTo(species)))
                                        .withRequestBody(matchingJsonPath("$.confidence"))
                                        .withRequestBody(matchingJsonPath("$.timestamp")));
                }
        }

        @Nested
        @DisplayName("Edge Cases and Error Handling")
        class EdgeCasesTests {

                @Test
                @DisplayName("Should handle empty user ID")
                void triggerWeatherSummary_withEmptyUserId_shouldAttemptRequest() {
                        // Arrange
                        String userId = "";
                        stubFor(post(urlEqualTo("/webhook/sentio-weather-summary"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerWeatherSummary(userId);

                        // Assert - Service doesn't validate, delegates to n8n
                        assertThat(result).isTrue();
                }

                @Test
                @DisplayName("Should handle null query in AI agent")
                void triggerAiAgent_withNullQuery_shouldAttemptRequest() {
                        // Arrange
                        String userId = "test-user";
                        String query = null;
                        stubFor(post(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .willReturn(aResponse().withStatus(200)
                                                        .withHeader("Content-Type", "application/json")
                                                        .withBody("{}")));

                        // Act
                        String result = n8nWorkflowTriggerService.triggerAiAgent(userId, query);

                        // Assert
                        assertThat(result).isEqualTo("Workflow executed");
                }

                @Test
                @DisplayName("Should handle connection refused")
                void triggerSightingsSummary_withConnectionRefused_shouldReturnFalse() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-sightings-summary"))
                                        .willReturn(aResponse()
                                                        .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerSightingsSummary("user-123");

                        // Assert
                        assertThat(result).isFalse();
                }

                @Test
                @DisplayName("Should handle empty response body")
                void triggerAnimalDetectionWorkflow_withEmptyResponse_shouldReturnTrue() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .willReturn(aResponse()
                                                        .withStatus(200)
                                                        .withBody("")));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow(
                                        "device-001", "Red Fox", 0.95f);

                        // Assert
                        assertThat(result).isTrue();
                }

                @Test
                @DisplayName("Should handle malformed JSON response for AI agent")
                void triggerAiAgent_withMalformedJson_shouldReturnNull() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .willReturn(aResponse()
                                                        .withStatus(200)
                                                        .withBody("{invalid json}")));

                        // Act
                        String result = n8nWorkflowTriggerService.triggerAiAgent("user-123", "query");

                        // Assert
                        assertThat(result).isNull();
                }

                @Test
                @DisplayName("Should handle redirect response")
                void triggerWeatherSummary_withRedirect_shouldFollow() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-weather-summary"))
                                        .willReturn(aResponse()
                                                        .withStatus(302)
                                                        .withHeader("Location", "/webhook/sentio-weather-summary-v2")));

                        stubFor(post(urlEqualTo("/webhook/sentio-weather-summary-v2"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerWeatherSummary("user-123");

                        // Assert - POST redirects are not automatically followed by default
                        assertThat(result).isFalse();
                }

                @Test
                @DisplayName("Should handle 201 Created response as success")
                void triggerSightingsSummary_with201Response_shouldReturnTrue() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-sightings-summary"))
                                        .willReturn(aResponse().withStatus(201)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerSightingsSummary("user-123");

                        // Assert
                        assertThat(result).isTrue();
                }

                @Test
                @DisplayName("Should handle 202 Accepted response as success")
                void triggerAnimalDetectionWorkflow_with202Response_shouldReturnTrue() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .willReturn(aResponse().withStatus(202)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow(
                                        "device-001", "Red Fox", 0.95f);

                        // Assert
                        assertThat(result).isTrue();
                }

                @Test
                @DisplayName("Should handle 204 No Content response as success")
                void triggerWeatherSummary_with204Response_shouldReturnTrue() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-weather-summary"))
                                        .willReturn(aResponse().withStatus(204)));

                        // Act
                        boolean result = n8nWorkflowTriggerService.triggerWeatherSummary("user-123");

                        // Assert
                        assertThat(result).isTrue();
                }
        }

        @Nested
        @DisplayName("HTTP Request Validation Tests")
        class HttpRequestValidationTests {

                @Test
                @DisplayName("Should send POST request for weather summary")
                void triggerWeatherSummary_shouldUsePOSTMethod() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-weather-summary"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        n8nWorkflowTriggerService.triggerWeatherSummary("user-123");

                        // Assert
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-weather-summary")));
                        verify(0, getRequestedFor(urlEqualTo("/webhook/sentio-weather-summary")));
                }

                @Test
                @DisplayName("Should include Content-Type header")
                void triggerSightingsSummary_shouldIncludeContentTypeHeader() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-sightings-summary"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        n8nWorkflowTriggerService.triggerSightingsSummary("user-123");

                        // Assert
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-sightings-summary"))
                                        .withHeader("Content-Type", equalTo("application/json")));
                }

                @Test
                @DisplayName("Should construct correct webhook URL")
                void triggerWeatherSummary_shouldConstructCorrectUrl() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-weather-summary"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        n8nWorkflowTriggerService.triggerWeatherSummary("user-123");

                        // Assert
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-weather-summary")));
                }

                @Test
                @DisplayName("Should construct correct AI agent URL")
                void triggerAiAgent_shouldConstructCorrectUrl() {
                        // Arrange
                        stubFor(post(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .willReturn(aResponse().withStatus(200).withBody("{}")));

                        // Act
                        n8nWorkflowTriggerService.triggerAiAgent("user-123", "query");

                        // Assert
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-ai-agent-groq")));
                }

                @Test
                @DisplayName("Should send JSON payload for all workflows")
                void allWorkflows_shouldSendJsonPayload() {
                        // Arrange
                        stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));

                        // Act
                        n8nWorkflowTriggerService.triggerWeatherSummary("user-1");
                        n8nWorkflowTriggerService.triggerSightingsSummary("user-2");
                        n8nWorkflowTriggerService.triggerAiAgent("user-3", "query");
                        n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow("device-1", "Fox", 0.9f);

                        // Assert
                        verify(4, postRequestedFor(anyUrl())
                                        .withHeader("Content-Type", equalTo("application/json")));
                }
        }

        @Nested
        @DisplayName("Payload Structure Tests")
        class PayloadStructureTests {

                @Test
                @DisplayName("Weather summary payload should contain only userId")
                void triggerWeatherSummary_payloadShouldContainUserId() {
                        // Arrange
                        String userId = "payload-user-123";
                        stubFor(post(urlEqualTo("/webhook/sentio-weather-summary"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        n8nWorkflowTriggerService.triggerWeatherSummary(userId);

                        // Assert
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-weather-summary"))
                                        .withRequestBody(matchingJsonPath("$.userId", equalTo(userId))));
                }

                @Test
                @DisplayName("AI agent payload should contain userId and query")
                void triggerAiAgent_payloadShouldContainUserIdAndQuery() {
                        // Arrange
                        String userId = "ai-payload-user";
                        String query = "What's the weather?";
                        stubFor(post(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .willReturn(aResponse().withStatus(200).withBody("{}")));

                        // Act
                        n8nWorkflowTriggerService.triggerAiAgent(userId, query);

                        // Assert
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-ai-agent-groq"))
                                        .withRequestBody(matchingJsonPath("$.userId", equalTo(userId)))
                                        .withRequestBody(matchingJsonPath("$.query", equalTo(query))));
                }

                @Test
                @DisplayName("Animal detection payload should contain all required fields")
                void triggerAnimalDetectionWorkflow_payloadShouldContainAllFields() {
                        // Arrange
                        String deviceId = "test-device";
                        String species = "Red Fox";
                        float confidence = 0.95f;
                        stubFor(post(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .willReturn(aResponse().withStatus(200)));

                        // Act
                        n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow(deviceId, species, confidence);

                        // Assert
                        verify(postRequestedFor(urlEqualTo("/webhook/sentio-animal-detected"))
                                        .withRequestBody(matchingJsonPath("$.deviceId", equalTo(deviceId)))
                                        .withRequestBody(matchingJsonPath("$.species", equalTo(species)))
                                        .withRequestBody(matchingJsonPath("$.confidence"))
                                        .withRequestBody(matchingJsonPath("$.timestamp")));
                }
        }
}
