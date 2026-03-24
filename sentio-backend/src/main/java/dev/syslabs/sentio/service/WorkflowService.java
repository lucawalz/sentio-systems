package dev.syslabs.sentio.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.syslabs.sentio.model.WorkflowResult;
import dev.syslabs.sentio.model.WorkflowType;
import dev.syslabs.sentio.repository.WorkflowResultRepository;
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

    @Transactional
    public WorkflowResult saveWorkflowResult(WorkflowResult result) {
        log.info("Saving workflow result of type: {}", result.getWorkflowType());

        // Delete old results of the same type to keep only recent ones
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        workflowResultRepository.deleteOldResultsByType(result.getWorkflowType(), cutoff);

        return workflowResultRepository.save(result);
    }

    public Optional<WorkflowResult> getCurrentResult() {
        return workflowResultRepository.findTopByOrderByTimestampDesc();
    }

    public Optional<WorkflowResult> getCurrentResultByType(WorkflowType type) {
        return workflowResultRepository.findTopByWorkflowTypeOrderByTimestampDesc(type);
    }

    public Optional<WorkflowResult> getCurrentSummary() {
        return getCurrentResultByType(WorkflowType.WEATHER_SUMMARY);
    }

    public List<WorkflowResult> getRecentResults() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return workflowResultRepository.findByTimestampAfterOrderByTimestampDesc(since);
    }

    public List<WorkflowResult> getRecentResultsByType(WorkflowType type) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return workflowResultRepository.findByWorkflowTypeAndTimestampAfterOrderByTimestampDesc(type, since);
    }

    public List<WorkflowResult> getRecentSummaries() {
        return getRecentResultsByType(WorkflowType.WEATHER_SUMMARY);
    }

    @Transactional
    public void cleanupOldResults() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        workflowResultRepository.deleteOldResults(cutoff);
        log.info("Cleaned up workflow results older than 7 days");
    }

    @Transactional
    public void cleanupOldResultsByType(WorkflowType type) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        workflowResultRepository.deleteOldResultsByType(type, cutoff);
        log.info("Cleaned up {} workflow results older than 7 days", type);
    }


    @Transactional
    public WorkflowResult saveUserWorkflowResult(String userId, WorkflowResult result) {
        result.setUserId(userId);
        log.info("Saving workflow result of type {} for user {}", result.getWorkflowType(), userId);

        // Delete old results of the same type for this user
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        workflowResultRepository.deleteOldResultsByUser(userId, cutoff);

        return workflowResultRepository.save(result);
    }

    public Optional<WorkflowResult> getCurrentWeatherSummary(String userId) {
        return workflowResultRepository.findTopByUserIdAndWorkflowTypeOrderByTimestampDesc(
                userId, WorkflowType.WEATHER_SUMMARY);
    }

    public Optional<WorkflowResult> getCurrentSightingsSummary(String userId) {
        return workflowResultRepository.findTopByUserIdAndWorkflowTypeOrderByTimestampDesc(
                userId, WorkflowType.SIGHTINGS_SUMMARY);
    }

    public List<WorkflowResult> getUserRecentResults(String userId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return workflowResultRepository.findByUserIdAndTimestampAfterOrderByTimestampDesc(userId, since);
    }

    public List<WorkflowResult> getUserRecentResultsByType(String userId, WorkflowType type) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return workflowResultRepository.findByUserIdAndWorkflowTypeAndTimestampAfterOrderByTimestampDesc(
                userId, type, since);
    }
}
