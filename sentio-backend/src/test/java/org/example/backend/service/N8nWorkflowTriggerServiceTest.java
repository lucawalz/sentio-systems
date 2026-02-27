package org.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link N8nWorkflowTriggerService}.
 */
@ExtendWith(MockitoExtension.class)
class N8nWorkflowTriggerServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private N8nWorkflowTriggerService n8nWorkflowTriggerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(n8nWorkflowTriggerService, "n8nWebhookBaseUrl", "http://localhost/webhook");
        ReflectionTestUtils.setField(n8nWorkflowTriggerService, "workflowSuffix", "-test");
    }

    @Nested
    @DisplayName("triggerWeatherSummary")
    class TriggerWeatherSummaryTests {

        @Test
        @DisplayName("should trigger weather summary and return true on success")
        void shouldTriggerWeatherSummaryAndReturnTrueOnSuccess() {
            when(restTemplate.exchange(
                    eq("http://localhost/webhook/sentio-weather-summary-test"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{ \"status\": \"success\" }", HttpStatus.OK));

            boolean result = n8nWorkflowTriggerService.triggerWeatherSummary("user-123");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false on 4xx error")
        void shouldReturnFalseOn4xxError() {
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)))
                    .thenReturn(new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST));

            boolean result = n8nWorkflowTriggerService.triggerWeatherSummary("user-123");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false on exception")
        void shouldReturnFalseOnException() {
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)))
                    .thenThrow(new RuntimeException("Connection error"));

            boolean result = n8nWorkflowTriggerService.triggerWeatherSummary("user-123");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("triggerSightingsSummary")
    class TriggerSightingsSummaryTests {

        @Test
        @DisplayName("should trigger sightings summary and return true on success")
        void shouldTriggerSightingsSummaryAndReturnTrueOnSuccess() {
            when(restTemplate.exchange(
                    eq("http://localhost/webhook/sentio-sightings-summary-test"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{ \"status\": \"success\" }", HttpStatus.OK));

            boolean result = n8nWorkflowTriggerService.triggerSightingsSummary("user-123");

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("triggerAiAgent")
    class TriggerAiAgentTests {

        @Test
        @DisplayName("should trigger AI agent and return response string on success")
        void shouldTriggerAiAgentAndReturnResponseOnSuccess() {
            Map<String, Object> responseBody = Map.of("response", "This is the AI answer");
            ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq("http://localhost/webhook/sentio-ai-agent-groq-test"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)))
                    .thenReturn(responseEntity);

            String result = n8nWorkflowTriggerService.triggerAiAgent("user-123", "What's up?");

            assertThat(result).isEqualTo("This is the AI answer");
        }

        @Test
        @DisplayName("should return default message if response field missing")
        void shouldReturnDefaultMessageIfResponseFieldMissing() {
            Map<String, Object> responseBody = Map.of("status", "ok");
            ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)))
                    .thenReturn(responseEntity);

            String result = n8nWorkflowTriggerService.triggerAiAgent("user-123", "What's up?");

            assertThat(result).isEqualTo("Workflow executed");
        }

        @Test
        @DisplayName("should return null on 4xx error")
        void shouldReturnNullOn4xxError() {
            ResponseEntity<Map> responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)))
                    .thenReturn(responseEntity);

            String result = n8nWorkflowTriggerService.triggerAiAgent("user-123", "What's up?");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null on exception")
        void shouldReturnNullOnException() {
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(Map.class)))
                    .thenThrow(new RuntimeException("Connection error"));

            String result = n8nWorkflowTriggerService.triggerAiAgent("user-123", "What's up?");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("triggerAnimalDetectionWorkflow")
    class TriggerAnimalDetectionWorkflowTests {

        @Test
        @DisplayName("should trigger animal detection and return true on success")
        void shouldTriggerAnimalDetectionAndReturnTrueOnSuccess() {
            when(restTemplate.exchange(
                    eq("http://localhost/webhook/sentio-animal-detected"), // This doesn't have suffix in implementation
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{ \"status\": \"success\" }", HttpStatus.OK));

            boolean result = n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow("device-1", "Fox", 1.0f);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false on 4xx error")
        void shouldReturnFalseOn4xxError() {
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)))
                    .thenReturn(new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST));

            boolean result = n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow("device-1", "Fox", 1.0f);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false on exception")
        void shouldReturnFalseOnException() {
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)))
                    .thenThrow(new RuntimeException("Connection error"));

            boolean result = n8nWorkflowTriggerService.triggerAnimalDetectionWorkflow("device-1", "Fox", 1.0f);

            assertThat(result).isFalse();
        }
    }
}
