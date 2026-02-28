
/**
 * Repository interface for workflow result data access.
 * Provides queries for retrieving results by type, time range, and recency.
 * Supports analytics and reporting for n8n workflow executions.
 */
package org.example.backend.repository;

import org.example.backend.model.WorkflowResult;
import org.example.backend.model.WorkflowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowResultRepository extends JpaRepository<WorkflowResult, Long> {

        /**
         * Finds the most recent workflow result of any type
         */
        Optional<WorkflowResult> findTopByOrderByTimestampDesc();

        /**
         * Finds the most recent workflow result of a specific type
         */
        Optional<WorkflowResult> findTopByWorkflowTypeOrderByTimestampDesc(WorkflowType workflowType);

        /**
         * Finds workflow results within a specific time range
         */
        List<WorkflowResult> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

        /**
         * Finds workflow results of a specific type within a time range
         */
        List<WorkflowResult> findByWorkflowTypeAndTimestampBetweenOrderByTimestampDesc(
                        WorkflowType workflowType, LocalDateTime start, LocalDateTime end);

        /**
         * Finds recent results since a given time
         */
        List<WorkflowResult> findByTimestampAfterOrderByTimestampDesc(LocalDateTime since);

        /**
         * Finds recent results of a specific type since a given time
         */
        List<WorkflowResult> findByWorkflowTypeAndTimestampAfterOrderByTimestampDesc(
                        WorkflowType workflowType, LocalDateTime since);

        /**
         * Deletes old results to maintain only recent data
         */
        @Modifying
        @Query("DELETE FROM WorkflowResult w WHERE w.timestamp < :cutoffTime")
        void deleteOldResults(@Param("cutoffTime") LocalDateTime cutoffTime);

        /**
         * Deletes old results of a specific type
         */
        @Modifying
        @Query("DELETE FROM WorkflowResult w WHERE w.workflowType = :type AND w.timestamp < :cutoffTime")
        void deleteOldResultsByType(@Param("type") WorkflowType type, @Param("cutoffTime") LocalDateTime cutoffTime);

        /**
         * Counts results generated today using timestamp range
         */
        @Query("SELECT COUNT(w) FROM WorkflowResult w WHERE w.timestamp >= :startOfDay AND w.timestamp < :endOfDay")
        long countTodaysResults(@Param("startOfDay") LocalDateTime startOfDay,
                        @Param("endOfDay") LocalDateTime endOfDay);

        /**
         * Counts results of a specific type generated today
         */
        @Query("SELECT COUNT(w) FROM WorkflowResult w WHERE w.workflowType = :type AND w.timestamp >= :startOfDay AND w.timestamp < :endOfDay")
        long countTodaysResultsByType(@Param("type") WorkflowType type, @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("endOfDay") LocalDateTime endOfDay);

        // ========== User-scoped queries ==========

        /**
         * Finds the most recent workflow result for a specific user and type
         */
        Optional<WorkflowResult> findTopByUserIdAndWorkflowTypeOrderByTimestampDesc(
                        String userId, WorkflowType workflowType);

        /**
         * Finds recent results for a specific user
         */
        List<WorkflowResult> findByUserIdAndTimestampAfterOrderByTimestampDesc(
                        String userId, LocalDateTime since);

        /**
         * Finds recent results for a specific user and type
         */
        List<WorkflowResult> findByUserIdAndWorkflowTypeAndTimestampAfterOrderByTimestampDesc(
                        String userId, WorkflowType workflowType, LocalDateTime since);

        /**
         * Deletes old results for a specific user
         */
        @Modifying
        @Query("DELETE FROM WorkflowResult w WHERE w.userId = :userId AND w.timestamp < :cutoffTime")
        void deleteOldResultsByUser(@Param("userId") String userId, @Param("cutoffTime") LocalDateTime cutoffTime);
}
