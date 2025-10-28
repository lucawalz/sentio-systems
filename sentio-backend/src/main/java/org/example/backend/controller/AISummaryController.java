package org.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.AISummary;
import org.example.backend.service.AISummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ai-summary")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "AI Summary", description = "AI Summary management API")
public class AISummaryController {

    private final AISummaryService aiSummaryService;

    /**
     * Gets the current AI summary
     */
    @Operation(summary = "Get current AI summary", description = "Retrieves the most recent AI summary", tags = {"Read"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the summary",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AISummary.class))),
            @ApiResponse(responseCode = "204", description = "No summary available",
                    content = @Content)
    })
    @GetMapping("/current")
    public ResponseEntity<AISummary> getCurrentSummary() {
        log.info("Fetching current AI summary");
        return aiSummaryService.getCurrentSummary()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Gets recent AI summaries from the last 24 hours
     */
    @Operation(summary = "Get recent AI summaries", description = "Retrieves AI summaries from the last 24 hours", tags = {"Read"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved summaries",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AISummary.class)))
    })
    @GetMapping("/recent")
    public ResponseEntity<List<AISummary>> getRecentSummaries() {
        log.info("Fetching recent AI summaries");
        List<AISummary> summaries = aiSummaryService.getRecentSummaries();
        return ResponseEntity.ok(summaries);
    }

    /**
     * Creates or updates an AI summary (used by Python script)
     */
    @Operation(summary = "Create AI summary", description = "Creates or updates an AI summary", tags = {"Create"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully created/updated the summary",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AISummary.class)))
    })
    @PostMapping
    public ResponseEntity<AISummary> createAISummary(@RequestBody AISummary aiSummary) {
        log.info("Creating new AI summary");
        if (aiSummary.getTimestamp() == null) {
            aiSummary.setTimestamp(LocalDateTime.now());
        }
        AISummary saved = aiSummaryService.saveAISummary(aiSummary);
        return ResponseEntity.ok(saved);
    }

    /**
     * Manual cleanup of old summaries
     */
    @Operation(summary = "Cleanup old AI summaries", description = "Manually triggers cleanup of old AI summaries", tags = {"Delete"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully completed cleanup",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupOldSummaries() {
        log.info("Manual cleanup of old AI summaries requested");
        aiSummaryService.cleanupOldSummaries();
        return ResponseEntity.ok("Cleanup completed successfully");
    }
}