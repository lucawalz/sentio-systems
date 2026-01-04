package org.example.backend.repository;

import jakarta.transaction.Transactional;
import org.example.backend.model.WeatherAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherAlertRepository extends JpaRepository<WeatherAlert, Long> {

        /** Find alert by unique BrightSky alert ID */
        Optional<WeatherAlert> findByAlertId(String alertId);

        /** Find all active alerts (not expired) */
        @Query("SELECT w FROM WeatherAlert w WHERE w.expires IS NULL OR w.expires > :now ORDER BY w.effective DESC")
        List<WeatherAlert> findActiveAlerts(@Param("now") LocalDateTime now);

        /** Find alerts by location coordinates within radius */
        @Query("SELECT w FROM WeatherAlert w WHERE " +
                        "w.latitude BETWEEN :latMin AND :latMax AND " +
                        "w.longitude BETWEEN :lonMin AND :lonMax " +
                        "ORDER BY w.effective DESC")
        List<WeatherAlert> findByLocationRadius(
                        @Param("latMin") Float latMin, @Param("latMax") Float latMax,
                        @Param("lonMin") Float lonMin, @Param("lonMax") Float lonMax);

        /** Find alerts by warn cell ID */
        List<WeatherAlert> findByWarnCellId(Long warnCellId);

        /** Find alerts by city */
        List<WeatherAlert> findByCityIgnoreCase(String city);

        /** Find alerts by severity level */
        List<WeatherAlert> findBySeverityOrderByEffectiveDesc(String severity);

        /** Find alerts effective within date range */
        @Query("SELECT w FROM WeatherAlert w WHERE w.effective BETWEEN :startDate AND :endDate ORDER BY w.effective DESC")
        List<WeatherAlert> findByEffectiveRange(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /** Find currently active alerts for a specific location */
        @Query("SELECT w FROM WeatherAlert w WHERE " +
                        "(w.city = :city OR w.warnCellId = :warnCellId) AND " +
                        "(w.expires IS NULL OR w.expires > :now) AND " +
                        "(w.effective IS NULL OR w.effective <= :now) " +
                        "ORDER BY w.severity DESC, w.effective DESC")
        List<WeatherAlert> findActiveAlertsForLocation(
                        @Param("city") String city,
                        @Param("warnCellId") Long warnCellId,
                        @Param("now") LocalDateTime now);

        /** Delete expired alerts */
        @Modifying
        @Transactional
        @Query("DELETE FROM WeatherAlert w WHERE w.expires < :cutoffDate")
        void deleteExpiredAlerts(@Param("cutoffDate") LocalDateTime cutoffDate);

        /** Delete alerts older than specified date */
        @Modifying
        @Transactional
        @Query("DELETE FROM WeatherAlert w WHERE w.createdAt < :cutoffDate")
        void deleteOldAlerts(@Param("cutoffDate") LocalDateTime cutoffDate);

        /** Count active alerts by severity */
        @Query("SELECT w.severity, COUNT(w) FROM WeatherAlert w WHERE " +
                        "(w.expires IS NULL OR w.expires > :now) " +
                        "GROUP BY w.severity")
        List<Object[]> countActiveBySeverity(@Param("now") LocalDateTime now);

        /** Find recent alerts (last 24 hours) */
        @Query("SELECT w FROM WeatherAlert w WHERE w.effective >= :since ORDER BY w.effective DESC")
        List<WeatherAlert> findRecentAlerts(@Param("since") LocalDateTime since);

        /** Check if alert exists by alertId */
        boolean existsByAlertId(String alertId);

        /** Get distinct cities with active alerts */
        @Query("SELECT DISTINCT w.city FROM WeatherAlert w WHERE " +
                        "w.city IS NOT NULL AND " +
                        "(w.expires IS NULL OR w.expires > :now)")
        List<String> findDistinctCitiesWithActiveAlerts(@Param("now") LocalDateTime now);

        // ===== Device-scoped queries =====

        /** Find active alerts for specific device */
        @Query("SELECT w FROM WeatherAlert w WHERE w.deviceId = :deviceId " +
                        "AND (w.expires IS NULL OR w.expires > :now) ORDER BY w.effective DESC")
        List<WeatherAlert> findActiveAlertsByDeviceId(
                        @Param("deviceId") String deviceId,
                        @Param("now") LocalDateTime now);

        /** Find all alerts for specific device */
        List<WeatherAlert> findByDeviceIdOrderByEffectiveDesc(String deviceId);
}