package dev.syslabs.sentio.service;

import dev.syslabs.sentio.dto.WeatherStats;
import dev.syslabs.sentio.model.RaspiWeatherData;
import dev.syslabs.sentio.repository.RaspiWeatherDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RaspiWeatherDataService}.
 * 
 * <p>
 * Following FIRST principles with Given/When/Then format.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class RaspiWeatherDataServiceTest {

    @Mock
    private RaspiWeatherDataRepository raspiWeatherDataRepository;

    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private RaspiWeatherDataService raspiWeatherDataService;

    private RaspiWeatherData createTestWeatherData(Long id) {
        RaspiWeatherData data = new RaspiWeatherData();
        data.setId(id);
        data.setTemperature(22.5f);
        data.setHumidity(65.0f);
        data.setPressure(1013.25f);
        data.setDeviceId("device-1");
        data.setTimestamp(LocalDateTime.now());
        return data;
    }

    @Nested
    @DisplayName("saveWeatherData")
    class SaveWeatherDataTests {

        @Test
        @DisplayName("should save weather data and return saved entity")
        void shouldSaveWeatherDataAndReturnSaved() {
            // Given
            RaspiWeatherData input = createTestWeatherData(null);
            input.setTimestamp(null);

            RaspiWeatherData saved = createTestWeatherData(1L);

            when(raspiWeatherDataRepository.save(any(RaspiWeatherData.class))).thenReturn(saved);

            // When
            RaspiWeatherData result = raspiWeatherDataService.saveWeatherData(input);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(raspiWeatherDataRepository).save(input);
        }

        @Test
        @DisplayName("should set timestamp if null")
        void shouldSetTimestampIfNull() {
            // Given
            RaspiWeatherData input = createTestWeatherData(null);
            input.setTimestamp(null);

            when(raspiWeatherDataRepository.save(any(RaspiWeatherData.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            RaspiWeatherData result = raspiWeatherDataService.saveWeatherData(input);

            // Then
            assertThat(result.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("should preserve existing timestamp")
        void shouldPreserveExistingTimestamp() {
            // Given
            LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0);
            RaspiWeatherData input = createTestWeatherData(null);
            input.setTimestamp(fixedTime);

            when(raspiWeatherDataRepository.save(any(RaspiWeatherData.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            RaspiWeatherData result = raspiWeatherDataService.saveWeatherData(input);

            // Then
            assertThat(result.getTimestamp()).isEqualTo(fixedTime);
        }
    }

    @Nested
    @DisplayName("getLatestWeatherData")
    class GetLatestWeatherDataTests {

        @Test
        @DisplayName("should return latest weather data for user's devices")
        void shouldReturnLatestForUserDevices() {
            // Given
            List<String> deviceIds = List.of("device-1", "device-2");
            RaspiWeatherData latest = createTestWeatherData(1L);

            when(deviceService.getMyDeviceIds()).thenReturn(deviceIds);
            when(raspiWeatherDataRepository.findTopByDeviceIdInOrderByTimestampDesc(deviceIds))
                    .thenReturn(latest);

            // When
            RaspiWeatherData result = raspiWeatherDataService.getLatestWeatherData();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should return null when user has no devices")
        void shouldReturnNullWhenNoDevices() {
            // Given
            when(deviceService.getMyDeviceIds()).thenReturn(Collections.emptyList());

            // When
            RaspiWeatherData result = raspiWeatherDataService.getLatestWeatherData();

            // Then
            assertThat(result).isNull();
            verify(raspiWeatherDataRepository, never()).findTopByDeviceIdInOrderByTimestampDesc(any());
        }
    }

    @Nested
    @DisplayName("getRecentWeatherData")
    class GetRecentWeatherDataTests {

        @Test
        @DisplayName("should return recent weather data for user's devices")
        void shouldReturnRecentDataForUserDevices() {
            // Given
            List<String> deviceIds = List.of("device-1");
            List<RaspiWeatherData> recentData = List.of(
                    createTestWeatherData(1L),
                    createTestWeatherData(2L));

            when(deviceService.getMyDeviceIds()).thenReturn(deviceIds);
            when(raspiWeatherDataRepository.findRecentDataByDevices(eq(deviceIds), any(LocalDateTime.class)))
                    .thenReturn(recentData);

            // When
            List<RaspiWeatherData> result = raspiWeatherDataService.getRecentWeatherData();

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when user has no devices")
        void shouldReturnEmptyListWhenNoDevices() {
            // Given
            when(deviceService.getMyDeviceIds()).thenReturn(Collections.emptyList());

            // When
            List<RaspiWeatherData> result = raspiWeatherDataService.getRecentWeatherData();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllWeatherData")
    class GetAllWeatherDataTests {

        @Test
        @DisplayName("should return all weather data for user's devices")
        void shouldReturnAllDataForUserDevices() {
            // Given
            List<String> deviceIds = List.of("device-1");
            List<RaspiWeatherData> allData = List.of(
                    createTestWeatherData(1L),
                    createTestWeatherData(2L),
                    createTestWeatherData(3L));

            when(deviceService.getMyDeviceIds()).thenReturn(deviceIds);
            when(raspiWeatherDataRepository.findByDeviceIdInOrderByTimestampDesc(deviceIds))
                    .thenReturn(allData);

            // When
            List<RaspiWeatherData> result = raspiWeatherDataService.getAllWeatherData();

            // Then
            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("getWeatherStats")
    class GetWeatherStatsTests {

        @Test
        @DisplayName("should return weather statistics for user's devices")
        void shouldReturnStatisticsForUserDevices() {
            // Given
            List<String> deviceIds = List.of("device-1");
            RaspiWeatherData latest = createTestWeatherData(1L);

            when(deviceService.getMyDeviceIds()).thenReturn(deviceIds);
            when(raspiWeatherDataRepository.countByDeviceIdIn(deviceIds)).thenReturn(100L);
            when(raspiWeatherDataRepository.findTopByDeviceIdInOrderByTimestampDesc(deviceIds))
                    .thenReturn(latest);
            when(raspiWeatherDataRepository.getAverageTemperatureSinceForDevices(eq(deviceIds),
                    any(LocalDateTime.class)))
                    .thenReturn(21.5);
            when(raspiWeatherDataRepository.getAverageHumiditySinceForDevices(eq(deviceIds), any(LocalDateTime.class)))
                    .thenReturn(60.0);
            when(raspiWeatherDataRepository.getAveragePressureSinceForDevices(eq(deviceIds), any(LocalDateTime.class)))
                    .thenReturn(1015.0);

            // When
            WeatherStats result = raspiWeatherDataService.getWeatherStats();

            // Then
            assertThat(result.getTotalReadings()).isEqualTo(100L);
            assertThat(result.getLatest()).isNotNull();
            assertThat(result.getAvgTemperature()).isEqualTo(21.5);
            assertThat(result.getAvgHumidity()).isEqualTo(60.0);
            assertThat(result.getAvgPressure()).isEqualTo(1015.0);
        }

        @Test
        @DisplayName("should return empty stats when user has no devices")
        void shouldReturnEmptyStatsWhenNoDevices() {
            // Given
            when(deviceService.getMyDeviceIds()).thenReturn(Collections.emptyList());

            // When
            WeatherStats result = raspiWeatherDataService.getWeatherStats();

            // Then
            assertThat(result.getTotalReadings()).isZero();
            assertThat(result.getLatest()).isNull();
            assertThat(result.getAvgTemperature()).isZero();
        }

        @Test
        @DisplayName("should handle null averages gracefully")
        void shouldHandleNullAverages() {
            // Given
            List<String> deviceIds = List.of("device-1");

            when(deviceService.getMyDeviceIds()).thenReturn(deviceIds);
            when(raspiWeatherDataRepository.countByDeviceIdIn(deviceIds)).thenReturn(0L);
            when(raspiWeatherDataRepository.findTopByDeviceIdInOrderByTimestampDesc(deviceIds))
                    .thenReturn(null);
            when(raspiWeatherDataRepository.getAverageTemperatureSinceForDevices(eq(deviceIds),
                    any(LocalDateTime.class)))
                    .thenReturn(null);
            when(raspiWeatherDataRepository.getAverageHumiditySinceForDevices(eq(deviceIds), any(LocalDateTime.class)))
                    .thenReturn(null);
            when(raspiWeatherDataRepository.getAveragePressureSinceForDevices(eq(deviceIds), any(LocalDateTime.class)))
                    .thenReturn(null);

            // When
            WeatherStats result = raspiWeatherDataService.getWeatherStats();

            // Then
            assertThat(result.getAvgTemperature()).isZero();
            assertThat(result.getAvgHumidity()).isZero();
            assertThat(result.getAvgPressure()).isZero();
        }
    }
}
