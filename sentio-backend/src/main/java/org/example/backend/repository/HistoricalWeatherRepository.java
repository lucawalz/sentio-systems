package org.example.backend.repository;

import jakarta.transaction.Transactional;
import org.example.backend.model.HistoricalWeather;
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
public interface HistoricalWeatherRepository extends JpaRepository<HistoricalWeather, Long> {

        @Query("SELECT h FROM HistoricalWeather h WHERE h.weatherDate >= :startDate AND h.weatherDate <= :endDate ORDER BY h.weatherDate ASC")
        List<HistoricalWeather> findByDateRange(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT h FROM HistoricalWeather h WHERE h.weatherDate = :date ORDER BY h.createdAt DESC")
        List<HistoricalWeather> findByWeatherDate(@Param("date") LocalDate date);

        HistoricalWeather findTopByWeatherDateOrderByCreatedAtDesc(LocalDate weatherDate);

        @Query("SELECT h FROM HistoricalWeather h WHERE h.city = :city AND h.weatherDate >= :date ORDER BY h.weatherDate ASC")
        List<HistoricalWeather> findByCityAndDateAfter(@Param("city") String city, @Param("date") LocalDate date);

        // Find existing historical weather by unique key (weatherDate + location)
        @Query("SELECT h FROM HistoricalWeather h WHERE h.weatherDate = :weatherDate AND h.city = :city AND h.country = :country")
        Optional<HistoricalWeather> findByWeatherDateAndLocation(
                        @Param("weatherDate") LocalDate weatherDate,
                        @Param("city") String city,
                        @Param("country") String country);

        @Query("SELECT h FROM HistoricalWeather h WHERE h.city = :city AND h.country = :country AND h.weatherDate BETWEEN :startDate AND :endDate")
        List<HistoricalWeather> findByLocationAndDateRange(
                        @Param("city") String city,
                        @Param("country") String country,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Delete historical weather older than specified days
        @Modifying
        @Transactional
        @Query("DELETE FROM HistoricalWeather h WHERE h.createdAt < :cutoffDate")
        void deleteOldHistoricalWeather(@Param("cutoffDate") LocalDateTime cutoffDate);

        // Delete historical weather for dates beyond retention period
        @Modifying
        @Transactional
        @Query("DELETE FROM HistoricalWeather h WHERE h.weatherDate < :cutoffDate")
        void deleteExpiredHistoricalWeather(@Param("cutoffDate") LocalDate cutoffDate);

        @Query("SELECT h FROM HistoricalWeather h WHERE h.createdAt >= :startDate ORDER BY h.createdAt DESC")
        List<HistoricalWeather> findRecentHistoricalWeather(@Param("startDate") LocalDateTime startDate);

        @Query("SELECT DISTINCT h.city FROM HistoricalWeather h WHERE h.weatherDate >= :date")
        List<String> findDistinctCitiesWithHistoricalWeather(@Param("date") LocalDate date);

        // Get historical weather for specific intervals (3 days ago, 1 week ago, etc.)
        @Query("SELECT h FROM HistoricalWeather h WHERE h.weatherDate IN :dates AND h.city = :city AND h.country = :country ORDER BY h.weatherDate DESC")
        List<HistoricalWeather> findByDatesAndLocation(
                        @Param("dates") List<LocalDate> dates,
                        @Param("city") String city,
                        @Param("country") String country);

        // Get count of historical records for monitoring
        @Query("SELECT COUNT(h) FROM HistoricalWeather h WHERE h.city = :city AND h.country = :country")
        long countByLocation(@Param("city") String city, @Param("country") String country);

        // Find missing historical dates for a location within a range (legacy - uses
        // city/country)
        @Query("SELECT DISTINCT h.weatherDate FROM HistoricalWeather h WHERE h.city = :city AND h.country = :country AND h.weatherDate BETWEEN :startDate AND :endDate ORDER BY h.weatherDate")
        List<LocalDate> findExistingDatesInRange(@Param("city") String city, @Param("country") String country,
                        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        // ========== Device-specific queries for multi-device support ==========

        // Find existing dates for a specific device
        @Query("SELECT DISTINCT h.weatherDate FROM HistoricalWeather h WHERE h.deviceId = :deviceId AND h.weatherDate BETWEEN :startDate AND :endDate ORDER BY h.weatherDate")
        List<LocalDate> findExistingDatesForDevice(@Param("deviceId") String deviceId,
                        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        // Find historical weather by date and device
        @Query("SELECT h FROM HistoricalWeather h WHERE h.weatherDate = :weatherDate AND h.deviceId = :deviceId")
        Optional<HistoricalWeather> findByWeatherDateAndDeviceId(
                        @Param("weatherDate") LocalDate weatherDate,
                        @Param("deviceId") String deviceId);

        // Find historical weather for specific dates and device
        @Query("SELECT h FROM HistoricalWeather h WHERE h.weatherDate IN :dates AND h.deviceId = :deviceId ORDER BY h.weatherDate DESC")
        List<HistoricalWeather> findByDatesAndDeviceId(
                        @Param("dates") List<LocalDate> dates,
                        @Param("deviceId") String deviceId);

        // Get count of historical records for a device
        @Query("SELECT COUNT(h) FROM HistoricalWeather h WHERE h.deviceId = :deviceId")
        long countByDeviceId(@Param("deviceId") String deviceId);
}