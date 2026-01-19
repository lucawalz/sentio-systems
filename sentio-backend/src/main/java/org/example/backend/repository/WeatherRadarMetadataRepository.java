package org.example.backend.repository;

import org.example.backend.model.WeatherRadarMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for weather radar metadata operations.
 * Provides methods for storing and querying radar snapshot summaries.
 */
@Repository
public interface WeatherRadarMetadataRepository extends JpaRepository<WeatherRadarMetadata, Long> {

    /**
     * Finds the most recent radar metadata entry.
     */
    Optional<WeatherRadarMetadata> findTopByOrderByTimestampDesc();

    /**
     * Finds radar metadata within a time range.
     */
    List<WeatherRadarMetadata> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime start, LocalDateTime end);

    /**
     * Finds recent radar metadata entries.
     */
    @Query("SELECT r FROM WeatherRadarMetadata r WHERE r.timestamp >= :since ORDER BY r.timestamp DESC")
    List<WeatherRadarMetadata> findRecentMetadata(LocalDateTime since);

    /**
     * Finds radar metadata for a specific location (within tolerance).
     */
    @Query("SELECT r FROM WeatherRadarMetadata r WHERE " +
           "ABS(r.latitude - :lat) < 0.1 AND ABS(r.longitude - :lon) < 0.1 " +
           "ORDER BY r.timestamp DESC")
    List<WeatherRadarMetadata> findByLocationApprox(Float lat, Float lon);

    /**
     * Finds entries with significant precipitation.
     */
    @Query("SELECT r FROM WeatherRadarMetadata r WHERE r.precipitationMax > :threshold " +
           "AND r.timestamp >= :since ORDER BY r.timestamp DESC")
    List<WeatherRadarMetadata> findWithSignificantPrecipitation(Float threshold, LocalDateTime since);

    /**
     * Gets average precipitation statistics for a time period.
     */
    @Query("SELECT AVG(r.precipitationAvg), AVG(r.coveragePercent) " +
           "FROM WeatherRadarMetadata r WHERE r.timestamp >= :since")
    Object[] getAverageStats(LocalDateTime since);

    /**
     * Deletes radar metadata older than the specified date.
     */
    @Modifying
    @Query("DELETE FROM WeatherRadarMetadata r WHERE r.createdAt < :before")
    void deleteOldMetadata(LocalDateTime before);

    /**
     * Counts entries in a time range.
     */
    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
