package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.WorkflowResult;
import org.example.backend.model.WorkflowType;
import org.example.backend.repository.WorkflowResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing workflow results from n8n executions.
 * Handles both AI summaries and agent responses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final WorkflowResultRepository workflowResultRepository;

    /**
     * Saves a workflow result with the specified type
     */
    @Transactional
    public WorkflowResult saveWorkflowResult(WorkflowResult result) {
        log.info("Saving workflow result of type: {}", result.getWorkflowType());

        // Delete old results of the same type to keep only recent ones
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        workflowResultRepository.deleteOldResultsByType(result.getWorkflowType(), cutoff);

        return workflowResultRepository.save(result);
    }

    /**
     * Gets the most recent workflow result (any type)
     */
    public Optional<WorkflowResult> getCurrentResult() {
        return workflowResultRepository.findTopByOrderByTimestampDesc();
    }

    /**
     * Gets the most recent result of a specific type
     */
    public Optional<WorkflowResult> getCurrentResultByType(WorkflowType type) {
        return workflowResultRepository.findTopByWorkflowTypeOrderByTimestampDesc(type);
    }

    /**
     * Gets the most recent AI summary
     */
    public Optional<WorkflowResult> getCurrentSummary() {
        return getCurrentResultByType(WorkflowType.WEATHER_SUMMARY);
    }

    /**
     * Gets recent results from the last 24 hours
     */
    public List<WorkflowResult> getRecentResults() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return workflowResultRepository.findByTimestampAfterOrderByTimestampDesc(since);
    }

    /**
     * Gets recent results of a specific type from the last 24 hours
     */
    public List<WorkflowResult> getRecentResultsByType(WorkflowType type) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return workflowResultRepository.findByWorkflowTypeAndTimestampAfterOrderByTimestampDesc(type, since);
    }

    /**
     * Gets recent AI summaries from the last 24 hours
     */
    public List<WorkflowResult> getRecentSummaries() {
        return getRecentResultsByType(WorkflowType.WEATHER_SUMMARY);
    }

    /**
     * Cleanup old results (older than 7 days)
     */
    @Transactional
    public void cleanupOldResults() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        workflowResultRepository.deleteOldResults(cutoff);
        log.info("Cleaned up workflow results older than 7 days");
    }

    /**
     * Cleanup old results of a specific type
     */
    @Transactional
    public void cleanupOldResultsByType(WorkflowType type) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        workflowResultRepository.deleteOldResultsByType(type, cutoff);
        log.info("Cleaned up {} workflow results older than 7 days", type);
    }

    // ========== User-scoped operations ==========

    /**
     * Saves a workflow result for a specific user
     */
    @Transactional
    public WorkflowResult saveUserWorkflowResult(String userId, WorkflowResult result) {
        result.setUserId(userId);
        log.info("Saving workflow result of type {} for user {}", result.getWorkflowType(), userId);

        // Delete old results of the same type for this user
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        workflowResultRepository.deleteOldResultsByUser(userId, cutoff);

        return workflowResultRepository.save(result);
    }

    /**
     * Gets the most recent weather summary for a user
     */
    public Optional<WorkflowResult> getCurrentWeatherSummary(String userId) {
        return workflowResultRepository.findTopByUserIdAndWorkflowTypeOrderByTimestampDesc(
                userId, WorkflowType.WEATHER_SUMMARY);
    }

    /**
     * Gets the most recent sightings summary for a user
     */
    public Optional<WorkflowResult> getCurrentSightingsSummary(String userId) {
        return workflowResultRepository.findTopByUserIdAndWorkflowTypeOrderByTimestampDesc(
                userId, WorkflowType.SIGHTINGS_SUMMARY);
    }

    /**
     * Gets recent results for a specific user
     */
    public List<WorkflowResult> getUserRecentResults(String userId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return workflowResultRepository.findByUserIdAndTimestampAfterOrderByTimestampDesc(userId, since);
    }

    /**
     * Gets recent results of a specific type for a user
     */
    public List<WorkflowResult> getUserRecentResultsByType(String userId, WorkflowType type) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return workflowResultRepository.findByUserIdAndWorkflowTypeAndTimestampAfterOrderByTimestampDesc(
                userId, type, since);
    }
}
