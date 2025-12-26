package org.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.WorkflowResult;
import org.example.backend.model.WorkflowType;
import org.example.backend.service.N8nWorkflowTriggerService;
import org.example.backend.service.WorkflowService;
import org.example.backend.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Workflow", description = "n8n Workflow results management API")
public class WorkflowController {

        private final WorkflowService workflowService;
        private final N8nWorkflowTriggerService n8nTriggerService;

        /**
         * Gets the current workflow result (most recent of any type)
         */
        @Operation(summary = "Get current workflow result", description = "Retrieves the most recent workflow result", tags = {
                        "Read" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the result", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowResult.class))),
                        @ApiResponse(responseCode = "204", description = "No result available", content = @Content)
        })
        @GetMapping("/current")
        public ResponseEntity<WorkflowResult> getCurrentResult() {
                log.info("Fetching current workflow result");
                return workflowService.getCurrentResult()
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.noContent().build());
        }

        /**
         * Gets the current AI summary
         */
        @Operation(summary = "Get current summary", description = "Retrieves the most recent AI summary", tags = {
                        "Read" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the summary", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowResult.class))),
                        @ApiResponse(responseCode = "204", description = "No summary available", content = @Content)
        })
        @GetMapping("/summaries/current")
        public ResponseEntity<WorkflowResult> getCurrentSummary() {
                log.info("Fetching current AI summary");
                return workflowService.getCurrentSummary()
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.noContent().build());
        }

        /**
         * Gets recent workflow results from the last 24 hours
         */
        @Operation(summary = "Get recent workflow results", description = "Retrieves workflow results from the last 24 hours", tags = {
                        "Read" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved results", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowResult.class)))
        })
        @GetMapping("/recent")
        public ResponseEntity<List<WorkflowResult>> getRecentResults() {
                log.info("Fetching recent workflow results");
                List<WorkflowResult> results = workflowService.getRecentResults();
                return ResponseEntity.ok(results);
        }

        /**
         * Gets recent AI summaries from the last 24 hours
         */
        @Operation(summary = "Get recent summaries", description = "Retrieves AI summaries from the last 24 hours", tags = {
                        "Read" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved summaries", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowResult.class)))
        })
        @GetMapping("/summaries/recent")
        public ResponseEntity<List<WorkflowResult>> getRecentSummaries() {
                log.info("Fetching recent AI summaries");
                List<WorkflowResult> summaries = workflowService.getRecentSummaries();
                return ResponseEntity.ok(summaries);
        }

        /**
         * Creates a workflow result (called by n8n)
         */
        @Operation(summary = "Create workflow result", description = "Creates a new workflow result (used by n8n)", tags = {
                        "Create" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully created the result", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowResult.class)))
        })
        @PostMapping
        public ResponseEntity<WorkflowResult> createWorkflowResult(@RequestBody WorkflowResult result) {
                log.info("Creating new workflow result of type: {}", result.getWorkflowType());
                if (result.getTimestamp() == null) {
                        result.setTimestamp(LocalDateTime.now());
                }
                if (result.getWorkflowType() == null) {
                        result.setWorkflowType(WorkflowType.SUMMARY);
                }
                WorkflowResult saved = workflowService.saveWorkflowResult(result);
                return ResponseEntity.ok(saved);
        }

        /**
         * Manual cleanup of old workflow results
         */
        @Operation(summary = "Cleanup old workflow results", description = "Manually triggers cleanup of old workflow results", tags = {
                        "Delete" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully completed cleanup", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class)))
        })
        @PostMapping("/cleanup")
        public ResponseEntity<String> cleanupOldResults() {
                log.info("Manual cleanup of old workflow results requested");
                workflowService.cleanupOldResults();
                return ResponseEntity.ok("Cleanup completed successfully");
        }

        // ========== User-scoped endpoints ==========

        /**
         * Gets the current weather summary for the authenticated user
         */
        @Operation(summary = "Get user's weather summary", description = "Retrieves the most recent weather summary for the authenticated user", tags = {
                        "User" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved weather summary", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowResult.class))),
                        @ApiResponse(responseCode = "204", description = "No weather summary available", content = @Content)
        })
        @GetMapping("/me/weather")
        public ResponseEntity<WorkflowResult> getMyWeatherSummary() {
                String userId = SecurityUtils.getCurrentUserId();
                log.info("Fetching weather summary for user: {}", userId);
                return workflowService.getCurrentWeatherSummary(userId)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.noContent().build());
        }

        /**
         * Gets the current sightings summary for the authenticated user
         */
        @Operation(summary = "Get user's sightings summary", description = "Retrieves the most recent sightings summary for the authenticated user", tags = {
                        "User" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved sightings summary", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowResult.class))),
                        @ApiResponse(responseCode = "204", description = "No sightings summary available", content = @Content)
        })
        @GetMapping("/me/sightings")
        public ResponseEntity<WorkflowResult> getMySightingsSummary() {
                String userId = SecurityUtils.getCurrentUserId();
                log.info("Fetching sightings summary for user: {}", userId);
                return workflowService.getCurrentSightingsSummary(userId)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.noContent().build());
        }

        /**
         * Gets all recent workflow results for the authenticated user
         */
        @Operation(summary = "Get user's recent results", description = "Retrieves all workflow results for the authenticated user from the last 24 hours", tags = {
                        "User" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved results", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowResult.class)))
        })
        @GetMapping("/me/recent")
        public ResponseEntity<List<WorkflowResult>> getMyRecentResults() {
                String userId = SecurityUtils.getCurrentUserId();
                log.info("Fetching recent results for user: {}", userId);
                List<WorkflowResult> results = workflowService.getUserRecentResults(userId);
                return ResponseEntity.ok(results);
        }

        /**
         * Creates a workflow result for the authenticated user (called by n8n with user
         * context)
         */
        @Operation(summary = "Create user workflow result", description = "Creates a new workflow result for a specific user (used by n8n)", tags = {
                        "Create" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully created the result", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowResult.class)))
        })
        @PostMapping("/me")
        public ResponseEntity<WorkflowResult> createUserWorkflowResult(@RequestBody WorkflowResult result) {
                String userId = SecurityUtils.getCurrentUserId();
                log.info("Creating workflow result of type {} for user: {}", result.getWorkflowType(), userId);
                if (result.getTimestamp() == null) {
                        result.setTimestamp(LocalDateTime.now());
                }
                WorkflowResult saved = workflowService.saveUserWorkflowResult(userId, result);
                return ResponseEntity.ok(saved);
        }

        // ========== Trigger endpoints ==========

        /**
         * Triggers generation of a weather summary for the authenticated user
         */
        @Operation(summary = "Generate weather summary", description = "Triggers n8n workflow to generate a weather summary for the authenticated user", tags = {
                        "Trigger" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "202", description = "Summary generation triggered", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
                        @ApiResponse(responseCode = "500", description = "Failed to trigger workflow", content = @Content)
        })
        @PostMapping("/generate/weather")
        public ResponseEntity<String> generateWeatherSummary() {
                String userId = SecurityUtils.getCurrentUserId();
                log.info("Triggering weather summary generation for user: {}", userId);

                boolean triggered = n8nTriggerService.triggerWeatherSummary(userId);
                if (triggered) {
                        return ResponseEntity.accepted().body("Weather summary generation triggered");
                } else {
                        return ResponseEntity.internalServerError().body("Failed to trigger workflow");
                }
        }

        /**
         * Triggers generation of a sightings summary for the authenticated user
         */
        @Operation(summary = "Generate sightings summary", description = "Triggers n8n workflow to generate a sightings summary for the authenticated user", tags = {
                        "Trigger" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "202", description = "Summary generation triggered", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
                        @ApiResponse(responseCode = "500", description = "Failed to trigger workflow", content = @Content)
        })
        @PostMapping("/generate/sightings")
        public ResponseEntity<String> generateSightingsSummary() {
                String userId = SecurityUtils.getCurrentUserId();
                log.info("Triggering sightings summary generation for user: {}", userId);

                boolean triggered = n8nTriggerService.triggerSightingsSummary(userId);
                if (triggered) {
                        return ResponseEntity.accepted().body("Sightings summary generation triggered");
                } else {
                        return ResponseEntity.internalServerError().body("Failed to trigger workflow");
                }
        }

        /**
         * Asks the AI Agent a question about the user's data
         */
        @Operation(summary = "Ask AI Agent", description = "Sends a question to the AI Agent and returns a response based on user's data", tags = {
                        "Agent" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Agent responded successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AgentResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Failed to get agent response", content = @Content)
        })
        @PostMapping("/agent/ask")
        public ResponseEntity<AgentResponse> askAgent(@RequestBody AgentQuery query) {
                String userId = SecurityUtils.getCurrentUserId();
                log.info("User {} asking agent: {}", userId, query.getQuery());

                String response = n8nTriggerService.triggerAiAgent(userId, query.getQuery());
                if (response != null) {
                        return ResponseEntity.ok(new AgentResponse(response, true));
                } else {
                        return ResponseEntity.internalServerError()
                                        .body(new AgentResponse("Failed to get agent response", false));
                }
        }

        /**
         * Request DTO for AI Agent queries
         */
        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class AgentQuery {
                private String query;
        }

        /**
         * Response DTO for AI Agent
         */
        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class AgentResponse {
                private String response;
                private boolean success;
        }
}
