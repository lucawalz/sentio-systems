
package org.example.backend.repository;

import org.example.backend.model.AISummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AISummaryRepository extends JpaRepository<AISummary, Long> {

    /**
     * Finds the most recent AI summary
     */
    Optional<AISummary> findTopByOrderByTimestampDesc();

    /**
     * Finds AI summaries within a specific time range
     */
    List<AISummary> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Finds recent summaries since a given time
     */
    List<AISummary> findByTimestampAfterOrderByTimestampDesc(LocalDateTime since);

    /**
     * Deletes old summaries to maintain only recent data
     */
    @Modifying
    @Query("DELETE FROM AISummary a WHERE a.timestamp < :cutoffTime")
    void deleteOldSummaries(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Counts summaries generated today using timestamp range
     */
    @Query("SELECT COUNT(a) FROM AISummary a WHERE a.timestamp >= :startOfDay AND a.timestamp < :endOfDay")
    long countTodaysSummaries(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}