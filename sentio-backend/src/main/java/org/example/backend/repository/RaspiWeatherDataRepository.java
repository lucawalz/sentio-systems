package org.example.backend.repository;

import org.example.backend.model.RaspiWeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RaspiWeatherDataRepository extends JpaRepository<RaspiWeatherData, Long> {

    @Query("SELECT w FROM RaspiWeatherData w WHERE w.timestamp >= :start ORDER BY w.timestamp DESC")
    List<RaspiWeatherData> findRecentData(LocalDateTime start);

    RaspiWeatherData findTopByOrderByTimestampDesc();

    List<RaspiWeatherData> findAllByOrderByTimestampDesc();

    @Query("SELECT w FROM RaspiWeatherData w WHERE w.timestamp >= :start AND w.timestamp <= :end ORDER BY w.timestamp DESC")
    List<RaspiWeatherData> findDataBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT AVG(w.temperature) FROM RaspiWeatherData w WHERE w.timestamp >= :start")
    Double getAverageTemperatureSince(LocalDateTime start);

    @Query("SELECT AVG(w.humidity) FROM RaspiWeatherData w WHERE w.timestamp >= :start")
    Double getAverageHumiditySince(LocalDateTime start);

    @Query("SELECT AVG(w.pressure) FROM RaspiWeatherData w WHERE w.timestamp >= :start")
    Double getAveragePressureSince(LocalDateTime start);

    // Filtered by Device IDs (Privacy)
    RaspiWeatherData findTopByDeviceIdInOrderByTimestampDesc(List<String> deviceIds);

    @Query("SELECT w FROM RaspiWeatherData w WHERE w.deviceId IN :deviceIds AND w.timestamp >= :start ORDER BY w.timestamp DESC")
    List<RaspiWeatherData> findRecentDataByDevices(List<String> deviceIds, LocalDateTime start);

    List<RaspiWeatherData> findByDeviceIdInOrderByTimestampDesc(List<String> deviceIds);

    Long countByDeviceIdIn(List<String> deviceIds);

    @Query("SELECT AVG(w.temperature) FROM RaspiWeatherData w WHERE w.deviceId IN :deviceIds AND w.timestamp >= :start")
    Double getAverageTemperatureSinceForDevices(List<String> deviceIds, LocalDateTime start);

    @Query("SELECT AVG(w.humidity) FROM RaspiWeatherData w WHERE w.deviceId IN :deviceIds AND w.timestamp >= :start")
    Double getAverageHumiditySinceForDevices(List<String> deviceIds, LocalDateTime start);

    @Query("SELECT AVG(w.pressure) FROM RaspiWeatherData w WHERE w.deviceId IN :deviceIds AND w.timestamp >= :start")
    Double getAveragePressureSinceForDevices(List<String> deviceIds, LocalDateTime start);
}