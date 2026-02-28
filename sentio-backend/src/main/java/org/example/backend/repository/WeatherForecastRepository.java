
/**
 * Repository interface for weather forecast data access.
 * Provides custom queries for date ranges, city-based lookups, and unique forecast keys.
 * Supports retrieval of upcoming and historical forecasts.
 */
package org.example.backend.repository;

import jakarta.transaction.Transactional;
import org.example.backend.model.WeatherForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherForecastRepository extends JpaRepository<WeatherForecast, Long> {

        @Query("SELECT w FROM WeatherForecast w WHERE w.forecastDate >= :startDate AND w.forecastDate <= :endDate ORDER BY w.forecastDate ASC")
        List<WeatherForecast> findByDateRange(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT w FROM WeatherForecast w WHERE w.forecastDate >= :date ORDER BY w.forecastDate ASC")
        List<WeatherForecast> findUpcomingForecasts(@Param("date") LocalDate date);

        @Query("SELECT w FROM WeatherForecast w WHERE w.forecastDate = :date ORDER BY w.createdAt DESC")
        List<WeatherForecast> findByForecastDate(@Param("date") LocalDate date);

        WeatherForecast findTopByForecastDateOrderByCreatedAtDesc(LocalDate forecastDate);

        @Query("SELECT w FROM WeatherForecast w WHERE w.city = :city AND w.forecastDate >= :date ORDER BY w.forecastDate ASC")
        List<WeatherForecast> findByCityAndDateAfter(@Param("city") String city, @Param("date") LocalDate date);

        // Find existing forecast by unique key (forecastDateTime + location)
        @Query("SELECT w FROM WeatherForecast w WHERE w.forecastDateTime = :forecastDateTime AND w.city = :city AND w.country = :country")
        Optional<WeatherForecast> findByForecastDateTimeAndLocation(
                        @Param("forecastDateTime") LocalDateTime forecastDateTime,
                        @Param("city") String city,
                        @Param("country") String country);

        @Query("SELECT w FROM WeatherForecast w WHERE w.city = :city AND w.country = :country AND w.forecastDate BETWEEN :startDate AND :endDate")
        List<WeatherForecast> findByLocationAndDateRange(
                        @Param("city") String city,
                        @Param("country") String country,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Delete forecasts older than 2 days (based on createdAt)
        @Modifying
        @Transactional
        @Query("DELETE FROM WeatherForecast w WHERE w.createdAt < :cutoffDate")
        void deleteOldForecasts(@Param("cutoffDate") LocalDateTime cutoffDate);

        // Delete forecasts that are too old based on forecast date (cleanup stale
        // future forecasts)
        @Modifying
        @Transactional
        @Query("DELETE FROM WeatherForecast w WHERE w.forecastDate < :cutoffDate")
        void deleteExpiredForecasts(@Param("cutoffDate") LocalDate cutoffDate);

        @Query("SELECT w FROM WeatherForecast w WHERE w.createdAt >= :startDate ORDER BY w.createdAt DESC")
        List<WeatherForecast> findRecentForecasts(@Param("startDate") LocalDateTime startDate);

        @Query("SELECT DISTINCT w.city FROM WeatherForecast w WHERE w.forecastDate >= :date")
        List<String> findDistinctCitiesWithUpcomingForecasts(@Param("date") LocalDate date);

        @Query("SELECT w FROM WeatherForecast w WHERE w.forecastDate BETWEEN :startDate AND :endDate ORDER BY w.forecastDate ASC, w.createdAt DESC")
        List<WeatherForecast> findLatestForecastsInRange(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Get count of forecasts for monitoring
        @Query("SELECT COUNT(w) FROM WeatherForecast w WHERE w.city = :city AND w.country = :country")
        long countByLocation(@Param("city") String city, @Param("country") String country);

        // ===== Device-scoped queries =====

        /** Find upcoming forecasts for specific device */
        List<WeatherForecast> findByDeviceIdAndForecastDateGreaterThanEqual(String deviceId, LocalDate date);

        /** Find forecasts for device within date range */
        @Query("SELECT w FROM WeatherForecast w WHERE w.deviceId = :deviceId " +
                        "AND w.forecastDate BETWEEN :startDate AND :endDate ORDER BY w.forecastDateTime")
        List<WeatherForecast> findByDeviceIdAndDateRange(
                        @Param("deviceId") String deviceId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /** Find latest forecast for a device */
        WeatherForecast findTopByDeviceIdOrderByForecastDateTimeDesc(String deviceId);
}