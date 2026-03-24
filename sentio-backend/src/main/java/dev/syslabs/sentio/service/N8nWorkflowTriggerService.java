package dev.syslabs.sentio.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for triggering n8n workflows via webhooks.
 * Workflows are triggered by POST requests to n8n webhook endpoints.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class N8nWorkflowTriggerService {

    private final RestTemplate restTemplate;

    @Value("${n8n.webhook.base-url:https://n8n.syslabs.dev/webhook}")
    private String n8nWebhookBaseUrl;

    @Value("${n8n.workflow.suffix:}")
    private String workflowSuffix;

    /**
     * Triggers the weather summary workflow for a specific user.
     *
     * @param userId The user's Keycloak ID
     * @return true if triggered successfully, false otherwise
     */
    public boolean triggerWeatherSummary(String userId) {
        return triggerWebhook("sentio-weather-summary" + workflowSuffix, userId);
    }

    /**
     * Triggers the sightings summary workflow for a specific user.
     *
     * @param userId The user's Keycloak ID
     * @return true if triggered successfully, false otherwise
     */
    public boolean triggerSightingsSummary(String userId) {
        return triggerWebhook("sentio-sightings-summary" + workflowSuffix, userId);
    }

    /**
     * Triggers the AI Agent workflow with a user query.
     *
     * @param userId The user's Keycloak ID
     * @param query  The user's question
     * @return The agent's response or null if failed
     */
    public String triggerAiAgent(String userId, String query) {
        String webhookUrl = n8nWebhookBaseUrl + "/sentio-ai-agent-groq" + workflowSuffix;
        log.info("Triggering AI Agent (Groq) for user: {} with query: {}", userId, query);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("query", query);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    request,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object agentResponse = response.getBody().get("response");
                if (agentResponse != null) {
                    log.info("AI Agent responded successfully for user: {}", userId);
                    return agentResponse.toString();
                }
                // Some response received
                return "Workflow executed";
            }
            log.warn("AI Agent did not return expected response for user: {}", userId);
            return null;
        } catch (Exception e) {
            log.error("Failed to trigger AI Agent: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Triggers an n8n workflow via webhook.
     *
     * @param webhookPath The webhook path (e.g., "sentio-sightings-summary")
     * @param userId      The user's Keycloak ID
     * @return true if triggered successfully
     */
    private boolean triggerWebhook(String webhookPath, String userId) {
        String webhookUrl = n8nWebhookBaseUrl + "/" + webhookPath;
        log.info("Triggering n8n webhook: {} for user: {}", webhookPath, userId);
        log.info("Exactly constructed webhookUrl: '{}'", webhookUrl);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    request,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully triggered webhook: {} for user: {}", webhookPath, userId);
                return true;
            } else {
                log.warn("Webhook trigger returned non-success status: {} for {}",
                        response.getStatusCode(), webhookPath);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to trigger n8n webhook: {} - {}", webhookPath, e.getMessage());
            return false;
        }
    }

    /**
     * Triggers the animal detection workflow for rare or high-confidence
     * detections.
     *
     * @param deviceId   The device that detected the animal
     * @param species    The detected species
     * @param confidence Detection confidence score
     * @return true if triggered successfully
     */
    public boolean triggerAnimalDetectionWorkflow(String deviceId, String species, float confidence) {
        String webhookUrl = n8nWebhookBaseUrl + "/sentio-animal-detected";
        log.info("Triggering animal detection workflow for {} ({}) with confidence {}",
                species, deviceId, confidence);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = new HashMap<>();
            payload.put("deviceId", deviceId);
            payload.put("species", species);
            payload.put("confidence", confidence);
            payload.put("timestamp", java.time.LocalDateTime.now().toString());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    request,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully triggered animal detection workflow for {}", species);
                return true;
            }
            log.warn("Animal detection webhook returned non-success: {}", response.getStatusCode());
            return false;
        } catch (Exception e) {
            log.error("Failed to trigger animal detection workflow: {}", e.getMessage());
            return false;
        }
    }
}
